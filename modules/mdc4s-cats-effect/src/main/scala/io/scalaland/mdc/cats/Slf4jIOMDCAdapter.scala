package io.scalaland.mdc.cats

import cats.effect.{IO, IOLocal}

import java.util as ju
import org.slf4j.spi.MDCAdapter
import io.scalaland.mdc.MDC

import scala.jdk.CollectionConverters.*

/** Based on OlegPy's solution: https://olegpy.com/better-logging-monix-1/, adapted to new Slf4j 2 and Cats Effect 3. */
final class Slf4jIOMDCAdapter private (local: IOLocal[MDC.Ctx]) extends MDCAdapter {
  // TODO: IOLocal[Map[String, List]]

  private def getMDC: MDC.Ctx = IOGlobal.getCurrent(local).getOrElse(Map.empty[String, String])
  private def setMDC(mdc: MDC.Ctx): Unit = IOGlobal.setTemporarily(local, mdc)
  private def update(f: MDC.Ctx => MDC.Ctx): Unit = setMDC(f(getMDC))

  override def put(key: String, `val`: String): Unit = update(_.updated(key, `val`))
  @SuppressWarnings(Array("org.wartremover.warts.Null")) // talking to Java interface
  override def get(key: String): String = getMDC.get(key).orNull
  override def remove(key: String): Unit = update(_.removed(key))
  override def clear(): Unit = setMDC(Map.empty)

  override def getCopyOfContextMap: ju.Map[String, String] = getMDC.asJava
  override def setContextMap(contextMap: ju.Map[String, String] @unchecked): Unit = setMDC(contextMap.asScala.toMap)

  override def pushByKey(key: String, value: String): Unit = ???
  override def popByKey(key: String): String = ???
  override def getCopyOfDequeByKey(key: String): ju.Deque[String] = ???
  override def clearDequeByKey(key: String): Unit = ???
}
object Slf4jIOMDCAdapter {

  /** Initialize MDC.mdcAdapter (with default scope) to our implementation. */
  def configure: IO[MDC[IO]] =
    for {
      local <- IOLocal(Map.empty[String, String])
      _ <- IO {
        val field = classOf[org.slf4j.MDC].getDeclaredField("mdcAdapter")
        field.setAccessible(true)
        field.set(null, new Slf4jIOMDCAdapter(local))
      }
    } yield new IOMDC(local)
}
