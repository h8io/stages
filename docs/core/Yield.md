# Yield

`Yield` is the immediate result returned by a [`Stage`](Stage.md) when it processes an input value.  
Every `Yield` bundles three things together:

- an optional output value of type `O`;
- a [`Status`](Status.md) describing how the stage completed;
- an [`Evolution`](Evolution.md) that knows which stage to use when the pipeline is ready to process the next input.

`Yield` is not the final answer the pipeline gives to the outside world — that is [`Outcome`](Outcome.md).
`Yield` is the internal value that flows from one stage to the next, carrying the continuation forward.

```scala mdoc
import h8io.stages.*

trait MockEvolution[-I, +O, +E] extends Evolution[I, O, E] {
  override def onSuccess(): Stage[I, O, E] = ???
  override def onComplete(): Stage[I, O, E] = ???
  override def onError(): Stage[I, O, E] = ???
  override def dispose(): Unit = ()
}
```

## Carrying a Value vs Carrying Nothing

There are exactly two variants.

`Yield.Some` is produced when the stage has a result to hand downstream. It carries the output value together with the
status and the evolution:

```scala mdoc
val some = Yield.Some(42, Status.Success, new MockEvolution[Int, Int, Nothing] {})
some.out
some.status
```

`Yield.None` is produced when the stage deliberately emits nothing — for example, a filter that drops a particular
input. There is no output value, but the status and the evolution are still present so the pipeline can continue
correctly:

```scala mdoc
val none = Yield.None[Int, String, String](Status.Error("filtered out"), new MockEvolution[Int, String, String] {})
none.status
```

When a `Yield.None` reaches a `Stage.AndThen`, the downstream stage is not applied to the current input.  
Instead it is wired into the evolution so that the full composed stage will be called when a value eventually
arrives.

## Evolving the Pipeline

Once the pipeline has processed an input and is ready for the next one, it calls `evolve()` on the `Yield` returned
by the last stage.  
`evolve()` looks at the `status` and dispatches to the corresponding branch of the `evolution`:

- `Status.Success` → `evolution.onSuccess()`
- `Status.Complete` → `evolution.onComplete()`
- `Status.Error` → `evolution.onError()`

The returned `Stage` becomes the pipeline for the next run.

```scala mdoc
object DoubleStage extends Stage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(in * 2, Status.Success, new MockEvolution[Int, Int, Nothing] {})

  override def skip(): Evolution[Int, Int, Nothing] = new MockEvolution[Int, Int, Nothing] {}
}

object ErrorRecovery extends Stage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(0, Status.Complete, new MockEvolution[Int, Int, Nothing] {})

  override def skip(): Evolution[Int, Int, Nothing] = new MockEvolution[Int, Int, Nothing] {}
}

val yldSuccess = Yield.Some(
  21,
  Status.Success,
  new MockEvolution[Int, Int, Nothing] {
    override def onSuccess(): Stage[Int, Int, Nothing] = DoubleStage
    override def onError(): Stage[Int, Int, Nothing] = ErrorRecovery
  })

val nextStage = yldSuccess.evolve()
```

Because the status is `Success`, the next stage is the one returned by `onSuccess()`, which is `DoubleStage`.  
Applying it to a new input produces the expected result:

```scala mdoc
nextStage(21)
```

## Transforming a Yield

`map` transforms all three components of a `Yield` in one step. Each component gets its own mapping function.  
This is mainly used inside pipeline combinators rather than in application code, but it is part of the public API:

```scala mdoc
val original = Yield.Some(
  10,
  Status.Success,
  new MockEvolution[Int, Int, Nothing] {})

val transformed = original.map(
  mapOut = _ * 3,
  mapStatus = _ => Status.Complete,
  mapEvolution = _ => new MockEvolution[Int, Int, Nothing] {})

transformed.out
transformed.status
```

For `Yield.None`, `map` applies `mapStatus` and `mapEvolution` as usual — `mapOut` is accepted for type-consistency
but is never invoked because there is no value to transform:

```scala mdoc
val noneTransformed = Yield.None[Int, Int, String](Status.Error("blocked"), new MockEvolution[Int, Int, String] {})
  .map(_ + 1, _ => Status.Complete, _ => new MockEvolution[Int, Int, Nothing] {})

noneTransformed.status
```
