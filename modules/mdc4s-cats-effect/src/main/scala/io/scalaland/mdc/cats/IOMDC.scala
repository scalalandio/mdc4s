package io.scalaland.mdc.cats

import cats.effect.{IO, IOLocal}
import io.scalaland.mdc.MDC

/** Requires tagless final with `IOGlobal.configuredStatePropagation` instead of `IO.asyncForIO` to work. */
final class IOMDC(local: IOLocal[MDC.Ctx]) extends MDC[IO] {

  override def get(key: String): IO[Option[String]] = local.get.map(_.get(key))
  override def set(key: String, value: String): IO[Unit] = local.update(_.updated(key, value))
}
