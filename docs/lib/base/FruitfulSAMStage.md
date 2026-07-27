# FruitfulSAMStage

`FruitfulSAMStage` is the fruitful counterpart of [`SAMStage`](SAMStage.md): a stage that is its own evolution and
always produces an output. `skip` and `evolve` both return `this`, `dispose` is a no-op by default, and `apply` is
the only abstract method.

Because every generation is this same instance, the always-an-output guarantee holds for the whole evolution
lineage — which is what `Stage.Fruitful` demands, and what
[`Fruitful`](Fruitful.md), the per-run guarantee, cannot give. That makes `FruitfulSAMStage` the cheapest way to get
a [`Stage.Fruitful`](../../core/classes/Stage.md): implement `apply`, return `this` as the evolution, and the
closure over generations is trivially satisfied.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

```scala mdoc
object Loudify extends FruitfulSAMStage.Endo[String, Nothing] {
  override def apply(in: String): Yield.Some.Fruitful[String, String, Nothing] =
    Yield.Some.Fruitful(in.toUpperCase + "!", Status.Success, this)
}

Loudify("hello")
Loudify("hello").evolve()   // still a Stage.Fruitful
```

The trait deliberately does not extend `SAMStage` or [`Stagnation`](Stagnation.md): both seal the very methods it
needs to narrow. It is a parallel branch of the immutability hierarchy, not a refinement of that one.

Most stages never need to mix it in directly — [`Fn`](Fn.md) and
[`FruitfulStaticStage`](FruitfulStaticStage.md) are built on top of it and seal `apply` as well. Reach for
`FruitfulSAMStage` when the status varies with something other than the input alone, or when the yield needs an
evolution other than `this`, as [`Countdown`](../std/Countdown.md) does.

`FruitfulSAMStage.Endo[T, E]` is the alias for the endomorphic case.
