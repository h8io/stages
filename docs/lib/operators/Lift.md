# Lift

`Lift` wraps the inner stage's optional output into an `Option`, making the result always present.

- `Yield.Some(v, ...)` → `Yield.Some(Some(v), ...)`
- `Yield.None(...)` → `Yield.Some(None, ...)`

Because `Lift` always emits a value, it extends [`Fruitful`](../base/Fruitful.md). The evolution is mapped so that
every continuation stage remains wrapped in `Lift`. `Lift` is the inverse of [`Unlift`](../projections/Unlift.md).

```scala mdoc
import h8io.stages.operators.*
import h8io.stages.projections.*
```

```scala mdoc
val lifted = Lift(Unlift[Int])
lifted(Some(7))
lifted(None)
```
