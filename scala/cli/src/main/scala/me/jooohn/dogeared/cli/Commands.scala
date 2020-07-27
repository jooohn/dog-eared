package me.jooohn.dogeared.cli

import com.monovore.decline.{Command, Opts}
import me.jooohn.dogeared.app.AppDesign
import sun.misc.Signal
import zio.{ExitCode, RIO, ZIO}

class Commands(design: AppDesign) {
  import design._

  val server: Command[RIO[zio.ZEnv, ExitCode]] =
    Command(
      name = "server",
      header = "Start HTTP server process"
    ) {
      Opts {
        design.server.compile.use { _ =>
          ZIO.effectAsync[Any, Nothing, Unit] { done =>
            Signal.handle(new Signal("INT"), _ => done(ZIO.unit))
          }
        } as ExitCode.success
      }
    }

}
