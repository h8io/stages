# Loop

`Loop` feeds the output of each successful step back as the input of the next, running the inner endomorphic
stage in a tail-recursive loop until it stops.

The loop terminates when:

- The stage yields `Status.Complete` — the loop exits, converting an error-free `Complete` back to `Success`.
- The stage yields `Yield.None(Status.Success, ...)` — no output was produced; the loop emits
  `Yield.None(Success, ...)`.

`Loop` is suited for in-process iterative computations where each step seeds the next.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.operators.*
```

```scala mdoc
object IncrUntil10 extends SAMStage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    if (in < 10) Yield.Some(in + 1, Status.Success, this)
    else         Yield.Some(in, Status.complete, this)
}

Loop(IncrUntil10)(3)
```

The loop increments from `3` to `10`, exits on `Complete`, and returns `Some(10, Success, ...)`.
