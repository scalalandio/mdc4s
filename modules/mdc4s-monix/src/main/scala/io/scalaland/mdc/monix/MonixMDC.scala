package io.scalaland.mdc.monix

import io.scalaland.mdc.MDC
import monix.execution.misc.Local
import monix.eval.Task

final class MonixMDC(local: Local[MDC.Ctx]) extends MDC[Task] {

  private def enable[A](fa: Task[A]): Task[A] = fa.executeWithOptions(_.enableLocalContextPropagation)

  def get(key: String): Task[Option[String]] = enable(Task(local().get(key)))
  def set(key: String, value: String): Task[Unit] = enable(Task(local := local().updated(key, value)))
}
