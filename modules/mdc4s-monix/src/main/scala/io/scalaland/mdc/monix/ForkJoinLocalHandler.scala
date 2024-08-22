package io.scalaland.mdc.monix

trait ForkJoinLocalHandler[A] {

  def onFork(original: A): A
  def onJoin(current: A, forked: A): A
}
object ForkJoinLocalHandler {

  def apply[A](onFork: A => A)(onJoin: (A, A) => A): ForkJoinLocalHandler[A] = {
    val f1 = onFork
    val f2 = onJoin
    new ForkJoinLocalHandler[A] {
      def onFork(original: A): A = f1(original)
      def onJoin(current: A, forked: A): A = f2(current, forked)
    }
  }
}
