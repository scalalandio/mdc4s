package io.scalaland.mdc.test

import org.slf4j.event.Level
import org.slf4j.event.Level.*
import org.slf4j.{Logger, Marker, MDC}

import scala.annotation.nowarn
import scala.collection.convert.ImplicitConversions.*

class TestLogger(name: String) extends Logger {

  var level = INFO

  var logs = List.empty[TestLogger.Entry]

  @nowarn
  private def log(level: Level, format: String, throwable: Option[Throwable] = None, marker: Option[Marker] = None)(
      args: Any*
  ) = synchronized {
    logs = logs :+ TestLogger.Entry(
      level = level,
      message = if (args.isEmpty) format else format.format(args*),
      throwable = throwable,
      marker = marker,
      mdc = MDC.getCopyOfContextMap.toMap
    )
  }

  override def getName: String = name

  // format: off
  override def isTraceEnabled: Boolean = level.compareTo(TRACE) <= 0
  override def trace(msg: String): Unit = log(TRACE, msg)()
  override def trace(format: String, arg: Any): Unit = log(TRACE, format)(arg)
  override def trace(format: String, arg1: Any, arg2: Any): Unit = log(TRACE, format)(arg1, arg2)
  override def trace(format: String, arguments: Any*): Unit = log(TRACE, format)(arguments*)
  override def trace(msg: String, t: Throwable): Unit = log(TRACE, msg, throwable = Some(t))()
  override def isTraceEnabled(marker: Marker): Boolean = level.compareTo(TRACE) <= 0
  override def trace(marker: Marker, msg: String): Unit = log(TRACE, msg, marker = Some(marker))()
  override def trace(marker: Marker, format: String, arg: Any): Unit = log(TRACE, format)(arg)
  override def trace(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = log(TRACE, format, marker = Some(marker))(arg1, arg2)
  override def trace(marker: Marker, format: String, arguments: Any*): Unit = log(TRACE, format, marker = Some(marker))(arguments*)
  override def trace(marker: Marker, msg: String, t: Throwable): Unit = log(TRACE, msg, throwable = Some(t), marker = Some(marker))()
  override def isDebugEnabled: Boolean = level.compareTo(DEBUG) <= 0
  override def debug(msg: String): Unit = log(DEBUG, msg)()
  override def debug(format: String, arg: Any): Unit = log(DEBUG, format)(arg)
  override def debug(format: String, arg1: Any, arg2: Any): Unit = log(DEBUG, format)(arg1, arg2)
  override def debug(format: String, arguments: Any*): Unit = log(DEBUG, format)(arguments*)
  override def debug(msg: String, t: Throwable): Unit = log(DEBUG, msg, throwable = Some(t))()
  override def isDebugEnabled(marker: Marker): Boolean = level.compareTo(DEBUG) <= 0
  override def debug(marker: Marker, msg: String): Unit = log(DEBUG, msg, marker = Some(marker))()
  override def debug(marker: Marker, format: String, arg: Any): Unit = log(DEBUG, format)(arg)
  override def debug(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = log(DEBUG, format, marker = Some(marker))(arg1, arg2)
  override def debug(marker: Marker, format: String, arguments: Any*): Unit = log(DEBUG, format, marker = Some(marker))(arguments*)
  override def debug(marker: Marker, msg: String, t: Throwable): Unit = log(DEBUG, msg, throwable = Some(t), marker = Some(marker))()
  override def isInfoEnabled: Boolean = level.compareTo(INFO) <= 0
  override def info(msg: String): Unit = log(INFO, msg)()
  override def info(format: String, arg: Any): Unit = log(INFO, format)(arg)
  override def info(format: String, arg1: Any, arg2: Any): Unit = log(INFO, format)(arg1, arg2)
  override def info(format: String, arguments: Any*): Unit = log(INFO, format)(arguments*)
  override def info(msg: String, t: Throwable): Unit = log(INFO, msg, throwable = Some(t))()
  override def isInfoEnabled(marker: Marker): Boolean = level.compareTo(INFO) <= 0
  override def info(marker: Marker, msg: String): Unit = log(INFO, msg, marker = Some(marker))()
  override def info(marker: Marker, format: String, arg: Any): Unit = log(INFO, format)(arg)
  override def info(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = log(INFO, format, marker = Some(marker))(arg1, arg2)
  override def info(marker: Marker, format: String, arguments: Any*): Unit = log(INFO, format, marker = Some(marker))(arguments*)
  override def info(marker: Marker, msg: String, t: Throwable): Unit = log(INFO, msg, throwable = Some(t), marker = Some(marker))()
  override def isWarnEnabled: Boolean = level.compareTo(WARN) <= 0
  override def warn(msg: String): Unit = log(WARN, msg)()
  override def warn(format: String, arg: Any): Unit = log(WARN, format)(arg)
  override def warn(format: String, arg1: Any, arg2: Any): Unit = log(WARN, format)(arg1, arg2)
  override def warn(format: String, arguments: Any*): Unit = log(WARN, format)(arguments*)
  override def warn(msg: String, t: Throwable): Unit = log(WARN, msg, throwable = Some(t))()
  override def isWarnEnabled(marker: Marker): Boolean = level.compareTo(WARN) <= 0
  override def warn(marker: Marker, msg: String): Unit = log(WARN, msg, marker = Some(marker))()
  override def warn(marker: Marker, format: String, arg: Any): Unit = log(WARN, format)(arg)
  override def warn(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = log(WARN, format, marker = Some(marker))(arg1, arg2)
  override def warn(marker: Marker, format: String, arguments: Any*): Unit = log(WARN, format, marker = Some(marker))(arguments*)
  override def warn(marker: Marker, msg: String, t: Throwable): Unit = log(WARN, msg, throwable = Some(t), marker = Some(marker))()
  override def isErrorEnabled: Boolean = level.compareTo(ERROR) <= 0
  override def error(msg: String): Unit = log(ERROR, msg)()
  override def error(format: String, arg: Any): Unit = log(ERROR, format)(arg)
  override def error(format: String, arg1: Any, arg2: Any): Unit = log(ERROR, format)(arg1, arg2)
  override def error(format: String, arguments: Any*): Unit = log(ERROR, format)(arguments*)
  override def error(msg: String, t: Throwable): Unit = log(ERROR, msg, throwable = Some(t))()
  override def isErrorEnabled(marker: Marker): Boolean = level.compareTo(ERROR) <= 0
  override def error(marker: Marker, msg: String): Unit = log(ERROR, msg, marker = Some(marker))()
  override def error(marker: Marker, format: String, arg: Any): Unit = log(ERROR, format)(arg)
  override def error(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = log(ERROR, format, marker = Some(marker))(arg1, arg2)
  override def error(marker: Marker, format: String, arguments: Any*): Unit = log(ERROR, format, marker = Some(marker))(arguments*)
  override def error(marker: Marker, msg: String, t: Throwable): Unit = log(ERROR, msg, throwable = Some(t), marker = Some(marker))()
  // format: on
}
object TestLogger {

  case class Entry(
      level: Level,
      message: String,
      throwable: Option[Throwable],
      marker: Option[Marker],
      mdc: Map[String, String]
  )
}
