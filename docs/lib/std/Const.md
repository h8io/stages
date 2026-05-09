# Const

`Const(out)` is a stage that always produces the same constant value, ignoring its input entirely.
It is a `Fn[Any, O]`, so it always yields `Status.Success` and is stateless and freely reusable.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val always42 = Const(42)
always42("ignored")
always42(())
```
