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
```

## The Three Status Branches

`onSuccess()`, `onComplete()`, and `onError()` each return the next `Stage` for the corresponding
[`Status`](Status.md) value.  
They are not called by application code directly; `Yield.evolve()` dispatches to the right branch based on the
status carried by the `Yield`.

A stage that does not change between generations can return itself from every branch:

```scala mdoc
object ParseInt extends Stage[String, Int, String] {
  private val alwaysSelf = new Evolution[String, Int, String] {
    override def onSuccess(): Stage[String, Int, String] = ParseInt
    override def onComplete(): Stage[String, Int, String] = ParseInt
    override def onError(): Stage[String, Int, String] = ParseInt
    override def dispose(): Unit = ()
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

Implementations that hold no external resources may leave `dispose()` as a no-op.

A stage that acquires a resource at construction time can release it in `dispose()`:

```scala mdoc
class ResourceStage(name: String) extends Stage[String, String, Nothing] {
  private var open = true
  println(s"[$name] acquired")

  private val evolution = new Evolution[String, String, Nothing] {
    override def onSuccess(): Stage[String, String, Nothing]  = ResourceStage.this
    override def onComplete(): Stage[String, String, Nothing] = ResourceStage.this
    override def onError(): Stage[String, String, Nothing]    = ResourceStage.this
    override def dispose(): Unit = {
      open = false
      println(s"[$name] released")
    }
  }

  override def apply(in: String): Yield[String, String, Nothing] = {
    require(open, s"[$name] already disposed")
    Yield.Some(s"[$name]($in)", Status.Success, evolution)
  }

  override def skip(): Evolution[String, String, Nothing] = evolution
}
```

`Stage.execute()` triggers `dispose()` automatically after the stage runs:

```scala mdoc
new ResourceStage("conn").execute("hello")
```

When multiple stages are composed into a pipeline, all `Evolution` methods — `onSuccess()`, `onComplete()`,
`onError()`, and `dispose()` — are called in the order opposite to the order in which stages are applied:
downstream first, then upstream.
This applies to `dispose()` for the obvious reason, but equally to the branch methods, since they too may
release or transition resources.
This ensures that a downstream stage can still access anything the upstream stage provides right up
until the moment the upstream stage is torn down:

```scala mdoc
val pipeline = new ResourceStage("upstream") ~> new ResourceStage("downstream")
pipeline.execute("hello")
```

## Composing Evolutions

When `Stage.AndThen` composes two stages via `~>`, it also composes their `Evolution` values using `compose`.
The result is an `Evolution.AndThen` whose branches are the sequential compositions of the corresponding branches
from both evolutions:

```
composed.onSuccess()  ==  self.onSuccess()  ~>  that.onSuccess()
composed.onComplete() ==  self.onComplete() ~>  that.onComplete()
composed.onError()    ==  self.onError()    ~>  that.onError()
```

All `Evolution` method calls in `Evolution.AndThen` follow the same order: pipeline-downstream first, then
pipeline-upstream — the reverse of the order in which stages are applied.
This applies equally to `onSuccess()`, `onComplete()`, `onError()`, and `dispose()`, since all of them may
release or transition resources held by the producing stage.  
The [Diagram](Diagram.md) section on finalization walks through a concrete example of why this matters.

`compose` is an implementation detail of `Stage.AndThen` and is not part of the typical application-level API.

## Adapting Stages with map

`map` produces a new `Evolution` whose branches are each transformed by a function applied to the stage the original
branch would have returned.  
Disposal is delegated unchanged to the wrapped evolution.

This is used internally when a `Yield.None` propagates through `Stage.AndThen`: because no output was produced,
the downstream stage cannot be invoked immediately, so it is folded into the evolution via `map` so that the entire
composed pipeline will be applied when the next input arrives.
