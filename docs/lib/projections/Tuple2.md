# Tuple2

`Tuple2.Left` and `Tuple2.Right` extract one element of a pair `(A, B)`. Because a tuple always has both elements
present, they always yield `Yield.Some` with `Status.Success` — so unlike the other projections they are
[`FruitfulStaticStage`](../base/FruitfulStaticStage.md)s rather than
[`Projection`](../base/Projection.md)s: the always-an-output guarantee is visible in their types.

Use `Tuple2.Left[T]` and `Tuple2.Right[T]` to obtain a typed
[`FruitfulStaticStage`](../base/FruitfulStaticStage.md)`[(T, ?), T, Nothing]` or
`FruitfulStaticStage[(?, T), T, Nothing]` respectively.

```scala mdoc
import h8io.stages.projections.*
```

```scala mdoc
Tuple2.Left[String](("hello", 42))
Tuple2.Right[Int](("hello", 42))
```
