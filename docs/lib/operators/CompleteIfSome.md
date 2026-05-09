# CompleteIfSome

`CompleteIfSome` is a polymorphic singleton [`Decoration`](../base/Alterator.md) that stops the pipeline as soon as
the decorated stage produces a value. It works by appending [`Complete`](../std/Complete.md) via `~>`: when the stage
yields [`Yield.Some`](../../core/classes/Yield.md), `Complete` immediately emits
[`Status.Complete`](../../core/classes/Status.md); when the stage yields `Yield.None`, `Complete` is skipped and the
pipeline continues normally.

`CompleteIfSome[I, O, E]` returns a typed `Decoration[I, O, E]` that can be applied to any matching stage.

```scala mdoc
import h8io.stages.operators.*
import h8io.stages.projections.*
```

```scala mdoc
val stage = Unlift[Int]
val completing = CompleteIfSome[Option[Int], Int, Nothing](stage)

completing(Some(42))  // Unlift produces Some(42), Complete fires: Some(42, Complete, ...)
completing(None)      // Unlift produces None, Complete skipped: None(Success, ...)
```
