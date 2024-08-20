package io.scalaland.mdc.monix

import io.scalaland.mdc.MDC
import monix.eval.Task

import java.util as ju
import monix.execution.misc.Local
import org.slf4j.spi.MDCAdapter

import scala.jdk.CollectionConverters.*

/** Based on OlegPy's solution: https://olegpy.com/better-logging-monix-1/, adapted to new Slf4j 2. */
final class Slf4jMonixMDCAdapter(local: Local[MDC.Ctx]) extends MDCAdapter {
  private def getMDC: MDC.Ctx = local()
  private def setMDC(mdc: MDC.Ctx): Unit = local := mdc
  private def update(f: MDC.Ctx => MDC.Ctx): Unit = setMDC(f(getMDC))

  override def put(key: String, `val`: String): Unit = update(_.updated(key, `val`))
  override def get(key: String): String = getMDC.get(key).orNull
  override def remove(key: String): Unit = update(_.removed(key))
  override def clear(): Unit = setMDC(Map.empty)

  override def getCopyOfContextMap: ju.Map[String, String] = getMDC.asJava
  override def setContextMap(contextMap: ju.Map[String, String] @unchecked): Unit = setMDC(contextMap.asScala.toMap)

  // TODO: Local[Map[String, List]]
  override def pushByKey(key: String, value: String): Unit = ???
  override def popByKey(key: String): String = ???
  override def getCopyOfDequeByKey(key: String): ju.Deque[String] = ???
  override def clearDequeByKey(key: String): Unit = ???
}
object Slf4jMonixMDCAdapter {

  // Initialize MDC.mdcAdapter (with default scope) to our implementation.
  def configure(): Task[MDC[Task]] = for {
    local <- Task.now(Local[MDC.Ctx](Map.empty))
    _ <- Task {
      val field = classOf[org.slf4j.MDC].getDeclaredField("mdcAdapter")
      field.setAccessible(true)
      field.set(null, new Slf4jMonixMDCAdapter(local))
    }
  } yield new MonixMDC(local)
}
