package io.scalaland.mdc.test

import scala.annotation.nowarn

@nowarn
class CatsEffectIOWithSlf4jMDCLoggingSpec extends munit.FunSuite {

  test("MDC should be propagated correctly") {
    // import tested libraries
    import cats.implicits.*
    import cats.effect.{IO, Async}
    import cats.effect.implicits.*
    import cats.effect.unsafe.implicits.*
    import org.slf4j.LoggerFactory
    import org.slf4j.event.Level
    import scala.concurrent.duration.*
    // import integrations
    import io.scalaland.mdc.MDC
    import io.scalaland.mdc.cats.*
    import io.scalaland.mdc.slf4j.*
    import org.slf4j.spi.MDCAdapter

    import scala.collection.convert.ImplicitConversions.*

    val test = {
      def program[F[_]: Async](mdcF: F[MDC[F]]) = for {
        // given
        // this is called via: program(IOMDC.configure[MDCAdapter])(IOGlobal.configuredStatePropagation))
        mdc <- mdcF
        currentLogger = "io.scalaland.mdc.slf4j_cats_effect"
        reqIDTag = "requestID"
        originalReqID = "123456789"
        forkedReqID = "987654321"
        logger = LoggerFactory.getLogger(currentLogger)
        // when
        _ <- Async[F].delay(logger.info("Log inside .flatMap before MDC"))
        _ <- mdc.set(reqIDTag, originalReqID)
        _ <- Async[F].delay(logger.info("Log inside .flatMap after MDC"))
        forked <- {
          for {
            _ <- Async[F].delay(logger.info("Log inside .start before MDC"))
            _ <- mdc.set(reqIDTag, forkedReqID)
            _ <- Async[F].delay(logger.info("Log inside .start after MDC"))
          } yield ()
        }.start
        _ <- Async[F].sleep(250.millis)
        _ <- Async[F].delay(logger.info("Log after .start and before .join"))
        reqID1 <- mdc.get(reqIDTag)
        _ <- forked.join
        reqID2 <- mdc.get(reqIDTag)
        _ <- Async[F].delay(logger.info("Log after .start and after .join"))
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
          logs
            .find(_.message == "Log inside .start before MDC")
            .map(_.mdc)
            .contains(Map(reqIDTag -> originalReqID, "forked" -> "yes")),
          "MDC should be inherited from the parent Fiber and modified by onFork call"
        )
        assert(
          logs
            .find(_.message == "Log inside .start after MDC")
            .map(_.mdc)
            .contains(Map(reqIDTag -> forkedReqID, "forked" -> "yes")),
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
        assert(reqID2 == None, "Joined Fiber modify current Fiber's MDC with onJoin call")
        assert(
          logs
            .find(_.message == "Log after .start and after .join")
            .map(_.mdc)
            .contains(
              Map(s"current.$reqIDTag" -> originalReqID, s"forked.$reqIDTag" -> forkedReqID, "forked.forked" -> "yes")
            ),
          "Joined Fiber does not modify current Fiber's MDC"
        )
        // format: off
        assert(
          logs == List(
            TestLogger.Entry(Level.INFO, "Log inside .flatMap before MDC", None, None, Map()),
            TestLogger.Entry(Level.INFO, "Log inside .flatMap after MDC", None, None, Map(reqIDTag -> originalReqID)),
            TestLogger.Entry(Level.INFO, "Log inside .start before MDC", None, None, Map(reqIDTag -> originalReqID, "forked" -> "yes")),
            TestLogger.Entry(Level.INFO, "Log inside .start after MDC", None, None, Map(reqIDTag -> forkedReqID, "forked" -> "yes")),
            TestLogger.Entry(Level.INFO, "Log after .start and before .join", None, None, Map(reqIDTag -> originalReqID)),
            TestLogger.Entry(Level.INFO, "Log after .start and after .join", None, None, Map(s"current.$reqIDTag" -> originalReqID, s"forked.$reqIDTag" -> forkedReqID, "forked.forked" -> "yes")),
          ),
          "All logs are present and in order"
        )
        // format: on
      }

      program(
        IOMDC.configure[MDCAdapter](
          // test onFork by adding forked -> yes entry
          onFork = ctx => ctx.updated("forked", "yes"),
          // test onJoin by prepending prefixed to values from both sides
          onJoin = (ctx1, ctx2) =>
            ctx1.map { case (k, v) => s"current.$k" -> v } ++ ctx2.map { case (k, v) => s"forked.$k" -> v }
        )
      )(IOGlobal.configuredStatePropagation)
    }

    test.unsafeRunSync()
  }
}
