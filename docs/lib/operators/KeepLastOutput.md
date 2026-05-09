# KeepLastOutput

`KeepLastOutput` remembers the last value produced by the inner stage and re-emits it when the inner stage
yields nothing.

The decorator has two states:

- **Initial state** (no value seen yet): [`Yield.None`](../../core/classes/Yield.md) is forwarded unchanged;
  `Yield.Some` is forwarded and transitions to the remembered state.
- **Remembered state**: regardless of whether the inner stage yields `Some` or `None`, the last known value is
  always emitted. A new `Yield.Some` updates the remembered value.

The factory `KeepLastOutput(stage)` always starts in the initial state.

```scala mdoc
import h8io.stages.operators.*
import h8io.stages.projections.*
```

```scala mdoc
val stage = KeepLastOutput(Unlift[Int])

val r1 = stage(Some(7))   // inner yields Some(7): emitted, value remembered
val r2 = r1.evolve()(None)    // inner yields None: last value re-emitted
val r3 = r2.evolve()(Some(3)) // inner yields Some(3): emitted, remembered value updated
```
