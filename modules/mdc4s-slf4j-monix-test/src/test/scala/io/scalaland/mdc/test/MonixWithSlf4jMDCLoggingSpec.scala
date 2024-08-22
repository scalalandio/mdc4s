package io.scalaland.mdc.test

import scala.annotation.nowarn

@nowarn
class MonixWithSlf4jMDCLoggingSpec extends munit.FunSuite {

  test("MDC should be propagated correctly") {
    // import tested libraries
    import cats.implicits.*
    import cats.effect.{ConcurrentEffect, IO, Timer}
    import cats.effect.implicits.*
    import monix.eval.Task
    import monix.execution.Scheduler.Implicits.global
    import org.slf4j.LoggerFactory
    import scala.concurrent.duration.*
    // import integrations
    import io.scalaland.mdc.MDC
    import io.scalaland.mdc.monix.*
    import io.scalaland.mdc.slf4j.*
    import org.slf4j.spi.MDCAdapter

    import scala.collection.convert.ImplicitConversions.*

    val test = {
      implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
      def program[F[_]: ConcurrentEffect](mdcF: F[MDC[F]]) = for {
        // given
        // this is called via: program(TaskMDC.configure[MDCAdapter])(TaskGlobal.configuredStatePropagation)
        mdc <- mdcF
        currentLogger = "io.scalaland.mdc.slf4j_cats_effect"
        reqIDTag = "requestID"
        originalReqID = "123456789"
        forkedReqID = "987654321"
        logger = LoggerFactory.getLogger(currentLogger)
        // when
        _ <- ConcurrentEffect[F].delay(logger.info("Log inside .flatMap before MDC"))
        _ <- mdc.set(reqIDTag, originalReqID)
        _ <- ConcurrentEffect[F].delay(logger.info("Log inside .flatMap after MDC"))
        forked <- {
          for {
            _ <- ConcurrentEffect[F].delay(logger.info("Log inside .start before MDC"))
            _ <- mdc.set(reqIDTag, forkedReqID)
            _ <- ConcurrentEffect[F].delay(logger.info("Log inside .start after MDC"))
          } yield ()
        }.start
        _ <- ConcurrentEffect[F].liftIO(cats.effect.IO.sleep(250.millis))
        _ <- ConcurrentEffect[F].delay(logger.info("Log after .start and before .join"))
        reqID1 <- mdc.get(reqIDTag)
        _ <- forked.join
        reqID2 <- mdc.get(reqIDTag)
        _ <- ConcurrentEffect[F].delay(logger.info("Log after .start and after .join"))
        logs = TestLoggerFactory.getLogger(currentLogger).logs
      } yield {
        // then
        assert(
          logs.find(_.message == "Log inside .flatMap before MDC").map(_.mdc).contains(Map()),
          "MDC should be empty initially"
        )
        assert(
          logs.find(_.message == "Log inside .flatMap after MDC").map(_.mdc).contains(Map(reqIDTag -> originalReqID)),
          "MDC.set(key, value) should add value to MDC in logs"
        )
        assert(
          logs.find(_.message == "Log inside .start before MDC").map(_.mdc).contains(Map(reqIDTag -> originalReqID)),
          "MDC should be inherited from the parent Fiber"
        )
        assert(
          logs.find(_.message == "Log inside .start after MDC").map(_.mdc).contains(Map(reqIDTag -> forkedReqID)),
          "MDC.set(key, value) should update value in MDC in logs"
        )
        assert(reqID1.contains(originalReqID), "Forked Fiber does not modify current Fiber's MDC")
        assert(
          logs
            .find(_.message == "Log after .start and before .join")
            .map(_.mdc)
            .contains(Map(reqIDTag -> originalReqID)),
          "Forked Fiber does not modify current Fiber's MDC"
        )
        assert(reqID2.contains(originalReqID), "Joined Fiber does not modify current Fiber's MDC")
        assert(
          logs
            .find(_.message == "Log after .start and after .join")
            .map(_.mdc)
            .contains(Map(reqIDTag -> originalReqID)),
          "Joined Fiber does not modify current Fiber's MDC"
        )

      }

      program(TaskMDC.configure[MDCAdapter])(TaskGlobal.configuredStatePropagation)
    }

    test.runSyncUnsafe()
  }
}
