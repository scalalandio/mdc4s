package io.scalaland.mdc.monix

import io.scalaland.mdc.MDC
import monix.execution.misc.Local
import monix.eval.Task

final class TaskMDC(local: Local[MDC.Ctx]) extends MDC[Task] {

  private def enable[A](fa: Task[A]): Task[A] = fa // .executeWithOptions(_.enableLocalContextPropagation)

  def get(key: String): Task[Option[String]] = enable(Task(local().get(key)))
  def set(key: String, value: String): Task[Unit] = enable(Task(local := local().updated(key, value)))
}
object TaskMDC {

  final private class TaskCtxManager(local: Local[MDC.Ctx]) extends MDC.CtxManager {

    def getMDC: MDC.Ctx = local()
    def setMDC(mdc: MDC.Ctx): Unit = local := mdc
  }

  /** Requires `task.executeWithOptions(_.enableLocalContextPropagation)`. */
  def configure[A: MDC.Initializer]: Task[MDC[Task]] = for {
    local <- Task.now(Local[MDC.Ctx](Map.empty))
    _ <- Task(MDC.Initializer[A](new TaskCtxManager(local)))
  } yield new TaskMDC(local)
}
