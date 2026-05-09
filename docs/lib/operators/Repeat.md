# Repeat

`Repeat` keeps re-applying the inner stage to the same input, following `Status.Success` transitions, until
the stage signals completion. On an error-free `Status.Complete` the status is converted back to `Success`;
if the `Complete` carries errors, those are preserved.

Unlike [`Loop`](Loop.md), `Repeat` does not feed output back as input — the same original input is passed on
every iteration. It is suited for stages like [`Countdown`](../std/Countdown.md) that batch a fixed number of
runs and signal `Complete` at the end of each batch.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
import h8io.stages.operators.*
```

```scala mdoc
Repeat(Countdown[String](3))("x")
```

`Countdown(3)` processes `"x"` three times: `Success`, `Success`, `Complete`. `Repeat` absorbs the `Complete`
and returns `Some("x", Success, Repeat(Countdown(3)))`, ready for the next invocation.
