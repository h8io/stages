# Unlift

`Unlift` is a [`Projection`](../base/Projection.md) for `Option[T]`, extracting the contained value when present and
producing no output when absent. In both cases the status is [`Status.Success`](../../core/classes/Status.md) —
absence of a value is not treated as an error.

`Unlift` is a polymorphic singleton. Use `Unlift[T]` to obtain a typed
[`Projection`](../base/Projection.md)`[Option[T], T]`.

```scala mdoc
import h8io.stages.projections.*
```

```scala mdoc
val stage = Unlift[Int]
stage(Some(7))
stage(None)
```
