package io.scalaland.mdc

import org.slf4j.spi.MDCAdapter

package object slf4j {

  implicit val slf4jMDCInitializer: MDC.Initializer[MDCAdapter] = Slf4jInitializer
}
