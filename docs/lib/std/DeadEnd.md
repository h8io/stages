# DeadEnd

`DeadEnd` is a terminal stage that never produces an output value. Every invocation returns
`Yield.None(Status.Complete)`, immediately signalling that the pipeline is finished.
The yield is pre-built and cached to avoid allocation on each call.

`DeadEnd` is the natural end-of-stream marker. It is also returned by `Countdown(n)` when `n ≤ 0`.

An optional disposal hook can be supplied at construction via `DeadEnd(_dispose)`. The companion object
`DeadEnd` is a default no-op instance.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
DeadEnd("anything")
DeadEnd(42)
```
