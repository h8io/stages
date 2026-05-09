# Fruitful

`Fruitful` expresses a weaker guarantee than [`Fn`](Fn.md): the stage always returns
[`Yield.Some`](../../core/classes/Yield.md), but it is free to choose any status and any evolution. The return type
of `apply` is narrowed to `Yield.Some[I, O, E]` so the compiler can track this guarantee statically, making
`Fruitful` stages safe to use wherever an output value is always expected.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

```scala mdoc
object Loudify extends Fruitful[String, String, Nothing] with SAMStage[String, String, Nothing] {
  override def apply(in: String): Yield.Some[String, String, Nothing] =
    Yield.Some(in.toUpperCase + "!", Status.Success, this)
}

Loudify("hello")
```

`Fruitful` is useful for binary operators that need to guarantee both branches always produce output, or for stages
that always yield a value but may still signal errors or transition to a different stage.
