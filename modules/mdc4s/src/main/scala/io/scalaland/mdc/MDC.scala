package io.scalaland.mdc

/** Abstracts away how we perform MCD from what effect F we use. */
trait MDC[F[_]] {

  def get(key: String): F[Option[String]]
  def set(key: String, value: String): F[Unit]
}
object MDC {

  def apply[F[_]](implicit mdc: MDC[F]): MDC[F] = mdc

  /** How we're representing context. */
  type Ctx = Map[String, String]

  /** Abstracts away how the logging library would manage the context. */
  trait CtxManager {
    def getMDC: MDC.Ctx
    def setMDC(mdc: MDC.Ctx): Unit
    def update(f: MDC.Ctx => MDC.Ctx): Unit = setMDC(f(getMDC))
  }

  /** Abstracts away how we would like to connect to some logging library. */
  trait Initializer[A] {
    def apply(ctxManager: CtxManager): Unit
  }
  object Initializer {
    def apply[A](ctxManager: CtxManager)(implicit adapter: Initializer[A]): Unit = adapter(ctxManager)
  }
}
