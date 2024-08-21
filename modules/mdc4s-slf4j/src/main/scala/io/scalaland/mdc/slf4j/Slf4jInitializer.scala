package io.scalaland.mdc.slf4j

import java.util as ju
import io.scalaland.mdc.MDC
import org.slf4j.spi.MDCAdapter

import scala.annotation.nowarn

private[slf4j] object Slf4jInitializer extends MDC.Initializer[MDCAdapter] {

  /** Based on OlegPy's solution: https://olegpy.com/better-logging-monix-1/, adapted to new Slf4j 2. */
  @nowarn
  private final class Impl(ctxManager: MDC.CtxManager) extends MDCAdapter {
    import ctxManager.*
    
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
  
  override def apply(ctxManager: MDC.CtxManager): Unit = {
    val field = classOf[org.slf4j.MDC].getDeclaredField("mdcAdapter")
    field.setAccessible(true)
    field.set(null, new Impl(ctxManager))
  }
}
