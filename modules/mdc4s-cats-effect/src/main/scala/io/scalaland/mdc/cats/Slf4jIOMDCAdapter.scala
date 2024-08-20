package io.scalaland.mdc.cats

import cats.effect.{IO, IOLocal}

import java.util as ju
import org.slf4j.spi.MDCAdapter
import io.scalaland.mdc.MDC

import scala.annotation.nowarn

@nowarn
/** Based on OlegPy's solution: https://olegpy.com/better-logging-monix-1/, adapted to new Slf4j 2 and Cats Effect 3. */
final class Slf4jIOMDCAdapter private (local: IOLocal[MDC.Ctx]) extends MDCAdapter {
  private def getMDC: MDC.Ctx = IOGlobal.getCurrent(local).getOrElse(Map.empty[String, String])
  private def setMDC(mdc: MDC.Ctx): Unit = IOGlobal.setTemporarily(local, mdc)
  private def update(f: MDC.Ctx => MDC.Ctx): Unit = setMDC(f(getMDC))

  override def put(key: String, `val`: String): Unit = update(_.updated(key, `val`))
  override def get(key: String): String = getMDC.get(key).orNull
  override def remove(key: String): Unit = update(_ - key)
  override def clear(): Unit = setMDC(Map.empty)

  import scala.collection.convert.ImplicitConversions.*
  override def getCopyOfContextMap: ju.Map[String, String] = getMDC
  override def setContextMap(contextMap: ju.Map[String, String] @unchecked): Unit = setMDC(contextMap.toMap)

  // TODO: IOLocal[Map[String, List]]
  override def pushByKey(key: String, value: String): Unit = ???
  override def popByKey(key: String): String = ???
  override def getCopyOfDequeByKey(key: String): ju.Deque[String] = ???
  override def clearDequeByKey(key: String): Unit = ???
}
object Slf4jIOMDCAdapter {

  /** Initialize MDC.mdcAdapter (with default scope) to our implementation. */
  def configure: IO[MDC[IO]] = for {
    local <- IOLocal(Map.empty[String, String])
    _ <- IO {
      val field = classOf[org.slf4j.MDC].getDeclaredField("mdcAdapter")
      field.setAccessible(true)
      field.set(null, new Slf4jIOMDCAdapter(local))
    }
  } yield new IOMDC(local)
}
