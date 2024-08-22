package io.scalaland.mdc.test

import io.scalaland.mdc.test.*
import org.slf4j.{ILoggerFactory, IMarkerFactory}
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.{MDCAdapter, SLF4JServiceProvider}

class TestLoggerProvider extends SLF4JServiceProvider {
  override def getLoggerFactory: ILoggerFactory = new TestLoggerFactory
  override def getMarkerFactory: IMarkerFactory = new BasicMarkerFactory
  override def getMDCAdapter: MDCAdapter = null
  override def getRequestedApiVersion: String = "2.0.99"
  override def initialize: Unit = ()
}
