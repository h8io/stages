# Evolution

`Evolution` is the strategy that decides which [`Stage`](Stage.md) to use when the pipeline is ready to process the
next input.  
Every [`Yield`](Yield.md) carries an `Evolution`, and when the pipeline has finished with one input it calls
`evolve()` on that `Yield`, which in turn selects the appropriate branch from the `Evolution` based on the
[`Status`](Status.md).

Three ideas make `Evolution` work:

- a status-driven dispatch that selects the next stage;
- a disposal contract that ties resource cleanup to the lifetime of the producing stage;
- a composition mechanism that wires together the evolutions of all stages in a pipeline.

```scala mdoc
import h8io.stages.*

trait MockEvolution[-I, +O, +E] extends Evolution[I, O, E] {
  override def onSuccess(): Stage[I, O, E] = ???
  override def onComplete(): Stage[I, O, E] = ???
  override def onError(): Stage[I, O, E] = ???
  override def dispose(): Unit = ()
}
```

## The Three Status Branches

`onSuccess()`, `onComplete()`, and `onError()` each return the next `Stage` for the corresponding
[`Status`](Status.md) value.  
They are not called by application code directly; `Yield.evolve()` dispatches to the right branch based on the
status carried by the `Yield`.

A stage that does not change between generations can return itself from every branch:

```scala mdoc
object ParseInt extends Stage[String, Int, String] {
  private val alwaysSelf = new MockEvolution[String, Int, String] {
    override def onSuccess(): Stage[String, Int, String] = ParseInt
    override def onComplete(): Stage[String, Int, String] = ParseInt
    override def onError(): Stage[String, Int, String] = ParseInt
  }

  override def apply(in: String): Yield[String, Int, String] =
    in.toIntOption match {
      case Some(n) => Yield.Some(n, Status.Success, alwaysSelf)
      case None    => Yield.None(Status.Error(s"not a number: $in"), alwaysSelf)
    }

  override def skip(): Evolution[String, Int, String] = alwaysSelf
}

val okResult  = ParseInt("42")
val errResult = ParseInt("hi")
```

Because `okResult` has status `Success`, `evolve()` calls `onSuccess()` and returns `ParseInt` again:

```scala mdoc
okResult.evolve()
```

Because `errResult` has status `Error`, `evolve()` calls `onError()` — also `ParseInt` here, since this stage
always returns the same next stage regardless of what happened:

```scala mdoc
errResult.evolve()
```

## Disposing Resources

`dispose()` releases all resources held by the stage that produced this `Evolution`.  
After `dispose()` is called the producing stage must be considered permanently unusable — it must not be
applied or skipped again.

Two situations guarantee `dispose()` will be called:

- `Stage.execute()` calls it immediately after the stage runs, since `execute` is a terminal operation and the
  continuation will never be needed.
- If a status branch method throws a `Throwable`, the caller is still required to call `dispose()` so that nothing
  is leaked even when evolution itself fails.

Implementations that hold no external resources may leave `dispose()` as a no-op, which is the default in
`MockEvolution` above.

## Composing Evolutions

When `Stage.AndThen` composes two stages via `~>`, it also composes their `Evolution` values using `compose`.
The result is an `Evolution.AndThen` whose branches are the sequential compositions of the corresponding branches
from both evolutions:

```
composed.onSuccess()  ==  self.onSuccess()  ~>  that.onSuccess()
composed.onComplete() ==  self.onComplete() ~>  that.onComplete()
composed.onError()    ==  self.onError()    ~>  that.onError()
```

The disposal order in `Evolution.AndThen` is pipeline-downstream first, then pipeline-upstream — the reverse of the
order in which stages are applied.  
This ordering is important: a downstream stage may depend on state or resources set up by the upstream stage, so it
must be finalized before the upstream stage is torn down.  
The [Diagram](Diagram.md) section on finalization walks through a concrete example of why this matters.

`compose` is an implementation detail of `Stage.AndThen` and is not part of the typical application-level API.

## Adapting Stages with map

`map` produces a new `Evolution` whose branches are each transformed by a function applied to the stage the original
branch would have returned.  
Disposal is delegated unchanged to the wrapped evolution.

This is used internally when a `Yield.None` propagates through `Stage.AndThen`: because no output was produced,
the downstream stage cannot be invoked immediately, so it is folded into the evolution via `map` so that the entire
composed pipeline will be applied when the next input arrives.
