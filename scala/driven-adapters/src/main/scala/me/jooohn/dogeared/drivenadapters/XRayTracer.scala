package me.jooohn.dogeared.drivenadapters

import cats.effect.{ContextShift, Sync}
import cats.syntax.all._
import com.amazonaws.xray.AWSXRayRecorder
import com.amazonaws.xray.entities.TraceHeader.SampleDecision
import com.amazonaws.xray.entities.{Segment, TraceHeader}
import me.jooohn.dogeared.drivenports.Tracer

import scala.jdk.CollectionConverters._
import scala.util.chaining._

case class XRayTracer[F[_]: Sync: ContextShift](recorder: AWSXRayRecorder) {
  private val F = Sync[F]

  def segment[A](name: String, request: TracingRequest, context: Option[TraceHeader])(
      run: Tracer[F] => F[(request.Response, A)]): F[A] =
    for {
      segment <- F.delay {
        val segment = context.fold(recorder.beginSegment(name)) { c =>
          c.getSampled match {
            case SampleDecision.NOT_SAMPLED =>
              recorder.beginDummySegment(name, c.getRootTraceId) tap { seg =>
                seg.setSampled(false)
              }
            case SampleDecision.SAMPLED =>
              recorder.beginSegment(name, c.getRootTraceId, c.getParentId) tap { seg =>
                seg.setSampled(true)
              }
            case _ =>
              recorder.beginSegment(name, c.getRootTraceId, c.getParentId)
          }
        }
        InternalSegment(segment)
      }
      attempt <- run(segment).attempt
      _ <- segment.end(request)(attempt.map(_._1))
      result <- F.fromEither(attempt.map(_._2))
    } yield result

  case class InternalSegment(segment: Segment) extends Tracer[F] {

    def span[A](name: String)(run: F[A]): F[A] =
      F.bracket(eval(recorder.beginSubsegment(name)))(_ => run) { subSegment =>
        eval(recorder.endSubsegment(subSegment))
      }

    def end(request: TracingRequest)(response: Either[Throwable, request.Response]): F[Unit] = eval {
      request.record(segment, response)
      recorder.endSegment()
    }

    def eval[A](run: => A): F[A] = F.delay {
      recorder.setTraceEntity(segment)
      run
    }

  }

}

case class TracingContext(traceId: String, parent: String)

sealed trait TracingRequest {
  type Response <: TracingResponse

  def record(segment: Segment, response: Either[Throwable, Response]): Unit
}
object TracingRequest {

  case class Http(method: String, url: String) extends TracingRequest {
    type Response = TracingResponse.Http

    override final def record(segment: Segment, response: Either[Throwable, TracingResponse.Http]): Unit = {
      segment.setHttp(
        Map[String, AnyRef](
          "request" -> Map(
            "method" -> method,
            "url" -> url,
          ).asJava,
          "response" -> response
            .fold(
              _ => Map("status" -> 500),
              r => Map("status" -> r.status)
            )
            .asJava,
        ).asJava)
      response match {
        case Left(_) => segment.setFault(true)
        case Right(r) =>
          r.status / 100 match {
            case 4 => segment.setError(true)
            case 5 => segment.setFault(true)
            case _ => ()
          }
      }
    }
  }
}

sealed trait TracingResponse
object TracingResponse {
  case class Http(status: Int) extends TracingResponse
}
