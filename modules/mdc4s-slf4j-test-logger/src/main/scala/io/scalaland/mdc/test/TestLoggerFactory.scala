package io.scalaland.mdc.test

import org.slf4j.{ILoggerFactory, Logger}

class TestLoggerFactory extends ILoggerFactory {

  override def getLogger(name: String): Logger = TestLoggerFactory.getLogger(name)
}
object TestLoggerFactory {

  var loggers = Map.empty[String, TestLogger]

  def getLogger(name: String): TestLogger = synchronized {
    if (!loggers.contains(name)) {
      loggers = loggers.updated(name, new TestLogger(name))
    }
    loggers(name)
  }
}
