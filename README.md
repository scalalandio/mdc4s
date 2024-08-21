# MDC4s

Hack-utility for setting up MDC context
  - in Slf4j
  - in such a way that it would get propagated along Cats Effect
  - without user having to manually extract some value from `IOLocal` just put it to some global available in methods
    which where we couldn't pass these vales by arguments (like every logging method from library not designed with
    Kleisli in mind)

## How it works?

TODO (OlegPy's solution: [https://olegpy.com/better-logging-monix-1/ (Web Archive)](https://web.archive.org/web/20230201063241/https://olegpy.com/better-logging-monix-1/) adapted to Cats Effect 3 and Slf4j 2).

## Would Cats Effect maintainers like it?

No. But they didn't let us propagate things in a sane way like
[Monix used to](https://monix.io/api/current/monix/eval/Task$$Options.html#enableLocalContextPropagation:monix.eval.Task.Options). 
