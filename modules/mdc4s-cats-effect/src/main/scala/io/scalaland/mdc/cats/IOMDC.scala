package io.scalaland.mdc.cats

import cats.effect.{IO, IOLocal}
import io.scalaland.mdc.MDC

final private class IOMDC private (local: IOLocal[MDC.Ctx]) extends MDC[IO] {

  override def get(key: String): IO[Option[String]] = local.get.map(_.get(key))
  override def set(key: String, value: String): IO[Unit] = local.update(_.updated(key, value))
}
object IOMDC {

  final private class IOCtxManager(local: IOLocal[MDC.Ctx]) extends MDC.CtxManager {

    def getMDC: MDC.Ctx = IOGlobal.getCurrent(local).getOrElse(Map.empty[String, String])
    def setMDC(mdc: MDC.Ctx): Unit = IOGlobal.setTemporarily(local, mdc)
  }

  /** Requires tagless final with `IOGlobal.configuredStatePropagation` instead of `IO.asyncForIO` to work. */
  def configure[A: MDC.Initializer](
      onFork: MDC.Ctx => MDC.Ctx = identity,
      onJoin: (MDC.Ctx, MDC.Ctx) => MDC.Ctx = (a, _) => a
  ): IO[MDC[IO]] = for {
    local <- IOLocal(Map.empty[String, String])
    _ <- IOGlobal.addHandler(local, ForkJoinLocalHandler(onFork)(onJoin))
    _ <- IO(MDC.Initializer[A](new IOCtxManager(local)))
  } yield new IOMDC(local)
}
