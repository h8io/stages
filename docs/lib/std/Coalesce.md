# Coalesce

`Coalesce` collapses an `Either[T, T]` to its contained value, regardless of which side holds it.
It is a `Fn[Either[T, T], T]`, so it always yields `Status.Success` and is stateless.

`Coalesce` is a polymorphic singleton. Use `Coalesce[T]` to obtain a typed `Fn[Either[T, T], T]`.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val stage = Coalesce[String]
stage(Left("hello"))
stage(Right("world"))
```
