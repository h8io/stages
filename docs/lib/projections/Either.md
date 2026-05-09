# Either

`Either.Left` and `Either.Right` are `Projection` stages for Scala's standard `scala.util.Either` type.
Each extracts one side of an `Either` and passes it downstream, or produces no output when the `Either` holds
the other side. In both cases the status is `Status.Success`, so the pipeline continues.

Use `Either.Left[T]` and `Either.Right[T]` to obtain a typed `Projection[Either[T, ?], T]` or
`Projection[Either[?, T], T]` respectively.

```scala mdoc
import h8io.stages.*
import h8io.stages.projections.*
```

```scala mdoc
Either.Left[String](Left("hello"))
Either.Left[String](Right(42))

Either.Right[Int](Left("hello"))
Either.Right[Int](Right(42))
```
