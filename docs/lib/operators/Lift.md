# Lift

`Lift` wraps the inner stage's optional output into an `Option`, making the result always present.

- `Yield.Some(v, ...)` → `Yield.Some(Some(v), ...)`
- `Yield.None(...)` → `Yield.Some(None, ...)`

Because `Lift` always emits a value, it is a [`Stage.Fruitful`](../../core/classes/Stage.md). The evolution is
mapped with `mapToFruitful` so that every continuation stage remains wrapped in `Lift` — which is what makes the
guarantee hold for the whole lineage even though the inner stage offers no such guarantee. `Lift` is the inverse of
[`Unlift`](../projections/Unlift.md).

```scala mdoc
import h8io.stages.operators.*
import h8io.stages.projections.*
```

```scala mdoc
val lifted = Lift(Unlift[Int])
lifted(Some(7))
lifted(None)
```
