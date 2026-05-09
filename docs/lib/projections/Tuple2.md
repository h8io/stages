# Tuple2

`Tuple2.Left` and `Tuple2.Right` are `Projection` stages for Scala's standard `scala.Tuple2` type.
Each extracts one element of a pair `(A, B)` and always yields `Yield.Some` with `Status.Success`,
because a tuple always has both elements present.

Use `Tuple2.Left[T]` and `Tuple2.Right[T]` to obtain a typed `Projection[(T, ?), T]` or
`Projection[(?, T), T]` respectively.

```scala mdoc
import h8io.stages.*
import h8io.stages.projections.*
```

```scala mdoc
Tuple2.Left[String](("hello", 42))
Tuple2.Right[Int](("hello", 42))
```
