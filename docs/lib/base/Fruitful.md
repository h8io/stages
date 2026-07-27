# Fruitful

`Fruitful` promises an output for **this** run: `apply` is narrowed to
[`Yield.Some`](../../core/classes/Yield.md)`[I, O, E]`, but the status, the evolution and everything the stage
evolves into are left free.

That freedom is the difference from [`Stage.Fruitful`](../../core/classes/Stage.md) in the core, which narrows
`skip` and the evolution as well and therefore carries the guarantee along the whole lineage. **Prefer
`Stage.Fruitful`**: the compiler keeps the guarantee across generations instead of dropping it after the first run,
and [`FruitfulSAMStage`](FruitfulSAMStage.md) makes it as cheap to implement as this trait.

`Fruitful` remains for the stages that are fruitful now but may evolve into something that is not.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

`Cache` in the examples module is the motivating case: while it holds a cached value it replays it on every input,
but a `Complete` status drops the cache and the next generation is a plain stage again. The guarantee is real for
the current run and genuinely absent for the next one:

```scala mdoc
final class Replay[I, O, E](out: O, alterand: Stage[I, O, E]) extends Fruitful[I, O, E] {
  override def apply(in: I): Yield.Some[I, O, E] = Yield.Some(out, Status.Success, skip())

  override def skip(): Evolution[I, O, E] = new Evolution[I, O, E] {
    override def evolve(status: Status[?]): Stage[I, O, E] = status match {
      case Status.Success => new Replay(out, alterand)   // fruitful again
      case _              => alterand                    // and here it is gone
    }
    override def dispose(): Unit = ()
  }
}
```

When the next generation is fruitful too — which is the common case — use
[`FruitfulSAMStage`](FruitfulSAMStage.md) or, for a static stage whose status varies with the input,
[`FruitfulStaticStage`](FruitfulStaticStage.md) instead.
