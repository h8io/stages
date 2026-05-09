# BreakIfSome

`BreakIfSome` is a polymorphic singleton [`Decoration`](../base/Alterator.md) that stops the pipeline as soon as
the decorated stage produces a value. It works by appending [`Break`](../std/Break.md) via `~>`: when the stage
yields [`Yield.Some`](../../core/classes/Yield.md), `Break` immediately emits
[`Status.Complete`](../../core/classes/Status.md); when the stage yields `Yield.None`, `Break` is skipped and the
pipeline continues normally.

`BreakIfSome[I, O, E]` returns a typed `Decoration[I, O, E]` that can be applied to any matching stage.

```scala mdoc
import h8io.stages.operators.*
import h8io.stages.projections.*
```

```scala mdoc
val stage = Unlift[Int]
val breaking = BreakIfSome[Option[Int], Int, Nothing](stage)

breaking(Some(42))  // Unlift produces Some(42), Break fires: Some(42, Complete, ...)
breaking(None)      // Unlift produces None, Break skipped: None(Success, ...)
```
