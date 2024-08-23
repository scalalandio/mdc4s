# MDC4s

Hack-utility for setting up MDC context
  - in Slf4j
  - in such a way that it would get propagated along Cats Effect
  - without user having to manually extract some value from `IOLocal` just put it to some global available in methods
    which where we couldn't pass these vales by arguments (like every logging method from library not designed with
    Kleisli in mind)

## Why ?

The problem (and solutions) were described in:

 * OlegPy's blog post: [https://olegpy.com/better-logging-monix-1/ (Web Archive)](https://web.archive.org/web/20230201063241/https://olegpy.com/better-logging-monix-1/)
 * SoftwareMill's blogpost: https://blog.softwaremill.com/correlation-ids-in-scala-using-monix-3aa11783db81

This attempt differs because:

 * original solution was implemented only for Monix, this one aims for both Monix Task and Cats Effect IO
   * at the time of writing this, the IO does not let you propagate context in unsafe parts of the code OOTB (like
     into `MDCAdapter`) so a decorator for a whole `Async[IO]` was needed  
 * uses tagless final - it allows using the same approach for both Task and IO, which could make the migration of
   tagless final codebases from Monix to CE# slightly easier
 * provides some utilities to modify context on forking and on joining - providing the same UX when working with Monix
   and CE3: 
   * OOTB Monix does not propagate context in `Local`s - when enabling it selectively `.start` will create a fiber
     without "main" `Fiber`s state. When we enable it everywhere... `.start` will propagate the context, but the `Fiber`
     and the "main" `Fiber` will write to the same context overriding each other
   * meanwhile `IOLocal` would copy the state on `.start` but provide no way of automatically merging values in the
     "main" `Fiber` and forked one
   * integrations provide some way of automatically modifying the `Local`/`IOLocal` on `.start`, and combining values
     from both `Fibers` on `.join`
 * is cross-compiled for 2.12, 2.13 AND 3 

## How it works?

```scala
// Cats Effect 3 example
import cats.effect.{IO, Async}
import io.scalaland.mdc.MDC

def program[F[_]: Async](mdcF: F[MDC[F]]): F[Result]

// import integrations
import io.scalaland.mdc.cats.*
import io.scalaland.mdc.slf4j.*
import org.slf4j.spi.MDCAdapter

program(
  // configures MDC, modifies MDCAdapted in Slf4j!!!
  IOMDC.configure[MDCAdapter](
    // an example of automatic modification of MDC context on .start, identity by default
    onFork = ctx => ctx.updated("forked", "yes"),
    // an example of automatic merging of MDCs from 2 fibers .onJoin, picks current fiber by default
    onJoin = (ctx1, ctx2) =>
      ctx1.map { case (k, v) => s"current.$k" -> v } ++ ctx2.map { case (k, v) => s"forked.$k" -> v }
  )
)(
  // replaces Async[IO] with a decorator doing the context propagation to ThreadLocals and handling context updates
  IOGlobal.configuredStatePropagation
)
```

```scala
// Monix 3 example
import cats.effect.ConcurrentEffect
import monix.eval.Task
import io.scalaland.mdc.MDC

def program[F[_]: ConcurrentEffect](mdcF: F[MDC[F]]): F[Result]

// import integrations
import io.scalaland.mdc.monix.*
import io.scalaland.mdc.slf4j.*
import org.slf4j.spi.MDCAdapter

program(
  // configures MDC, modifies MDCAdapted in Slf4j!!!
  TaskMDC.configure[MDCAdapter](
    // an example of automatic modification of MDC context on .start, identity by default
    onFork = ctx => ctx.updated("forked", "yes"),
    // an example of automatic merging of MDCs from 2 fibers .onJoin, picks current fiber by default
    onJoin = (ctx1, ctx2) =>
      ctx1.map { case (k, v) => s"current.$k" -> v } ++ ctx2.map { case (k, v) => s"forked.$k" -> v }
  )
)(
  // replaces ConcurrentEffect[Task] with a decorator configuring the context propagation and handling context updates
  TaskGlobal.configuredStatePropagation
)
```

## Would Cats Effect maintainers like it?

No. But they didn't let us propagate things in a sane way like
[Monix used to](https://monix.io/api/current/monix/eval/Task$$Options.html#enableLocalContextPropagation:monix.eval.Task.Options). At least not before https://github.com/typelevel/cats-effect/pull/3636 is merged.

Also I want something working with any library not just a few "blessed" ones.
