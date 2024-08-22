package io.scalaland.mdc.monix

import cats.effect.{CancelToken, ConcurrentEffect, ExitCase, Fiber, IO, SyncIO}
import monix.eval.Task
import monix.eval.instances.CatsConcurrentEffectForTask
import monix.execution.Scheduler
import monix.execution.misc.Local

import scala.annotation.nowarn

object TaskGlobal {

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

    // methods we had to wrap to DISABLE propagation of context between parent and child fiber
    override def start[A](fa: Task[A]): Task[Fiber[Task, A]] =
      Task(Local.getContext()).flatMap { ctx =>
        (Task(Local.setContext(ctx)) >> fa)
          .executeWithOptions(_.disableLocalContextPropagation)
          .start
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
