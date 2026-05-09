# CompleteIfNone

`CompleteIfNone` is a decorator that stops the pipeline when the inner stage produces no output.

When the inner stage yields [`Yield.None`](../../core/classes/Yield.md) with a
[`Status.Success`](../../core/classes/Status.md) status, `CompleteIfNone` upgrades the status to
[`Status.Complete`](../../core/classes/Status.md), signalling that processing should not continue. All other yields
are forwarded unchanged. The evolution is mapped in both cases so that continuations remain wrapped in
`CompleteIfNone`.

This is useful for stages that return `None` when their input stream is exhausted — wrapping them with
`CompleteIfNone` turns the absence of output into a clean pipeline termination.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.operators.*
```

```scala mdoc
object FindPositive extends SAMStage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    if (in > 0) Yield.Some(in, Status.Success, this) else Yield.None(Status.Success, this)
}

val stage = CompleteIfNone(FindPositive)
stage(5)   // inner yields Some: forwarded unchanged
stage(-3)  // inner yields None(Success): upgraded to None(Complete)
```
