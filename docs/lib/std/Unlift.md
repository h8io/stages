# Unlift

`Unlift` extracts the value from an `Option`, forwarding it when present and producing no output when absent.
In both cases the status is [`Status.Success`](../../core/classes/Status.md) — absence of a value is not treated as
an error.

`Unlift` is a polymorphic singleton. Use `Unlift[T]` to obtain a typed
[`Stage`](../../core/classes/Stage.md)`[Option[T], T, Nothing]`.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val stage = Unlift[Int]
stage(Some(7))
stage(None)
```
