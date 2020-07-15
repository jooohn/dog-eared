package me.jooohn.dogeared.drivenadapters

import cats.effect.Sync
import com.typesafe.scalalogging.{CanLog, LoggerTakingImplicit, Logger => ScalaLogging}
import me.jooohn.dogeared.drivenports.Logger
import org.slf4j.MDC

case class ScalaLoggingLogger(underlying: LoggerTakingImplicit[LoggingContext])(
    implicit context: LoggingContext = LoggingContext.empty)
    extends Logger {

  override def info[F[_]: Sync](message: => String): F[Unit] = delay(underlying.info(message))

  override def error[F[_]: Sync](message: => String): F[Unit] = delay(underlying.error(message))

  override def error[F[_]: Sync](throwable: Throwable): F[Unit] =
    delay(underlying.error(throwable.getMessage, throwable))

  def delay[F[_]: Sync](f: => Unit): F[Unit] = Sync[F].delay(f)

  override def withContext(mapping: (String, Any)*): Logger = copy()(context.append(mapping: _*))
}
object ScalaLoggingLogger {

  def of(name: String): ScalaLoggingLogger = ScalaLoggingLogger(ScalaLogging.takingImplicit(name))

}

case class LoggingContext(map: Map[String, Any]) {

  def append(mapping: (String, Any)*): LoggingContext = copy(map ++ mapping)

  def foreach(f: (String, Any) => Unit): Unit = map.foreach(f.tupled)

}
object LoggingContext {

  val empty: LoggingContext = LoggingContext(Map.empty)

  implicit val loggingContextCanLog: CanLog[LoggingContext] =
    new CanLog[LoggingContext] {
      override def logMessage(originalMsg: String, a: LoggingContext): String = {
        a.foreach((key, value) => MDC.put(key, value.toString))
        originalMsg
      }

      override def afterLog(a: LoggingContext): Unit =
        a.foreach((key, _) => MDC.remove(key))
    }
}
