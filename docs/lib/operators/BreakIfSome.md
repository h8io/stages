# BreakIfSome

`BreakIfSome` is a polymorphic singleton `Decoration` that stops the pipeline as soon as the decorated stage
produces a value. It works by appending [`Break`](../std/Break.md) via `~>`: when the stage yields `Yield.Some`,
`Break` immediately emits `Status.Complete`; when the stage yields `Yield.None`, `Break` is skipped and the
pipeline continues normally.

`BreakIfSome[I, O, E]` returns a typed `Decoration[I, O, E]` that can be applied to any matching stage.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
import h8io.stages.operators.*
```

```scala mdoc
val stage = Unlift[Int]
val breaking = BreakIfSome[Option[Int], Int, Nothing](stage)

breaking(Some(42))  // Unlift produces Some(42), Break fires: Some(42, Complete, ...)
breaking(None)      // Unlift produces None, Break skipped: None(Success, ...)
```
