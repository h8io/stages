# Identity

`Identity` is a polymorphic singleton: a single [`Fn.Endo`](../base/Fn.md)`[Any]` instance that passes its input
through unchanged, always yielding [`Status.Success`](../../core/classes/Status.md). Use `Identity[T]` to obtain a
typed `Fn.Endo[T]`.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val stage = Identity[String]
stage("hello")
stage.skip()
```
