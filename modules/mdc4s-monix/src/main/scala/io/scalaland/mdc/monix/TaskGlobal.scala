package io.scalaland.mdc.monix

import cats.effect.{CancelToken, ConcurrentEffect, ExitCase, Fiber, IO, SyncIO}
import monix.eval.Task
import monix.eval.instances.CatsConcurrentEffectForTask
import monix.execution.Scheduler
import monix.execution.misc.Local

import scala.annotation.nowarn

object TaskGlobal {

  private var handlers = Map.empty[Local[Any], ForkJoinLocalHandler[Any]]

  def addHandler[A](local: Local[A], handler: ForkJoinLocalHandler[A]): Task[Unit] = Task {
    // TODO: something proper instead of synchronized
    synchronized {
      handlers = handlers.updated(local.asInstanceOf[Local[Any]], handler.asInstanceOf[ForkJoinLocalHandler[Any]])
    }
  }

  private def onFork: Task[Unit] = Task {
    handlers.foreach { case (local, handler) =>
      local := handler.onFork(local.get)
    }
  }

  private def forked: Map[Local[Any], Any] =
    handlers.keys.flatMap(local => local.value.map(local -> _)).toMap

  private def onJoin(forked: Map[Local[Any], Any]): Task[Unit] = Task {
    handlers.foreach { case (local, handler) =>
      forked.get(local).foreach { forkedValue =>
        local := local.value.fold(forkedValue)(handler.onJoin(_, forkedValue))
      }
    }
  }

  private def propagateState[A](fa: Task[A]): Task[A] = fa.executeWithOptions(_.enableLocalContextPropagation)

  def configureStatePropagation(tc: ConcurrentEffect[Task]): ConcurrentEffect[Task] = new ConcurrentEffect[Task] {

    // methods we had to wrap to extract whole state, put it into TheadLocal, run, extract from ThreadLocal into state
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = propagateState(tc.flatMap(fa)(f))
    override def tailRecM[A, B](a: A)(f: A => Task[Either[A, B]]): Task[B] = propagateState(tc.tailRecM(a)(f))
    override def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = propagateState(
      tc.handleErrorWith(fa)(f)
    )
    @nowarn
    override def suspend[A](thunk: => Task[A]): Task[A] = propagateState(tc.suspend(thunk))

    // methods we had to wrap to control the propagation of values between patent and child fiber
    override def start[A](fa: Task[A]): Task[Fiber[Task, A]] =
      Task(Local.getContext()).flatMap { ctx =>
        (Task(Local.setContext(ctx.isolate())) >> onFork >> fa.map(a => (a -> forked)))
          .executeWithOptions(_.disableLocalContextPropagation)
          .start
          .map { fiber =>
            new Fiber[Task, A] {
              override def cancel: CancelToken[Task] = fiber.cancel
              override def join: Task[A] = fiber.join.flatMap { case (a, forked) =>
                onJoin(forked).as(a)
              }
            }
          }
      }

    // methods where user would not be able to read state, so they don't have to propagate it
    override def raiseError[A](e: Throwable): Task[A] = tc.raiseError(e)
    override def pure[A](x: A): Task[A] = tc.pure(x)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Task[A] = tc.async(k)
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => Task[Unit]): Task[A] = tc.asyncF(k)
    override def bracketCase[A, B](acquire: Task[A])(use: A => Task[B])(
        release: (A, ExitCase[Throwable]) => Task[Unit]
    ): Task[B] = tc.bracketCase(acquire)(use)(release)
    override def racePair[A, B](fa: Task[A], fb: Task[B]): Task[Either[(A, Fiber[Task, B]), (Fiber[Task, A], B)]] =
      tc.racePair(fa, fb)

    override def runCancelable[A](fa: Task[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[Task]] =
      tc.runCancelable(fa)(cb)
    override def runAsync[A](fa: Task[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] = tc.runAsync(fa)(cb)
  }

  def configuredStatePropagation: ConcurrentEffect[Task] = {
    val scheduler = Scheduler.global
    val options = Task.defaultOptions.withSchedulerFeatures(scheduler)
    configureStatePropagation(new CatsConcurrentEffectForTask()(scheduler, options))
  }
}
