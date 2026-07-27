# Countdown

`Countdown` is an endomorphic stage that passes its input through for exactly `n` invocations and then
signals pipeline completion. On each of the first `n − 1` calls it yields `Status.Success`; on the `n`-th
call it yields `Status.Complete` and the next generation resets to `n`, ready to count down again.

`Countdown` is fully immutable: each state transition creates a new instance rather than mutating the current one.
It never drops a value — the input is passed through on every call, including the completing one — so it is a
[`Stage.Fruitful`](../../core/classes/Stage.md), and so is every generation it counts down to.

The factory is the exception: `Countdown(n)` is typed as a plain `Stage.Endo`, because for `n ≤ 0` it returns
[`DeadEnd`](DeadEnd.md), which yields nothing at all.

The factory `Countdown(n)` is the normal entry point. If `n ≤ 0` it returns [`DeadEnd`](DeadEnd.md) instead,
ensuring the returned stage is always safe to use.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val c = Countdown[String](3)

val y1 = c("a")
val y2 = y1.evolve()("b")
val y3 = y2.evolve()("c")
```

`y1` and `y2` carry `Status.Success`; `y3` carries `Status.Complete`. Calling `y3.evolve()` returns a fresh
`Countdown(3)` that is ready to count down again.
