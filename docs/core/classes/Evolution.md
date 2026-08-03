# Evolution

`Evolution` is the strategy that decides which [`Stage`](Stage.md) to use when the pipeline is ready to process the
next input.  
Every [`Yield`](Yield.md) carries an `Evolution`, and when the pipeline has finished with one input it calls
`evolve()` on that `Yield`, which in turn calls the `Evolution` with the current [`Status`](Status.md).

Three ideas make `Evolution` work:

- a status-driven dispatch that selects the next stage;
- a disposal contract that ties resource cleanup to the lifetime of the producing stage;
- a composition mechanism that wires together the evolutions of all stages in a pipeline.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

## Selecting the Next Stage

`evolve(status)` returns the next `Stage` based on the given status.  
It is not called by application code directly; `Yield.evolve()` passes the `Yield`'s own status to it automatically.

A stage that does not change between generations can return itself regardless of the status:

```scala mdoc
object ParseInt extends Stage[String, Int, String] {
  private val alwaysSelf = new Evolution[String, Int, String] {
    override def evolve(status: Status[?]): Stage[String, Int, String] = ParseInt
    override def dispose(): Unit = ()
  }

  override def apply(in: String): Yield[String, Int, String] =
    in.toIntOption match {
      case Some(n) => Yield.Some(n, Status.Success, alwaysSelf)
      case None    => Yield.None(Status.error(s"not a number: $in"), alwaysSelf)
    }

  override def skip(): Evolution[String, Int, String] = alwaysSelf
}

val okResult  = ParseInt("42")
val errResult = ParseInt("hi")
```

Calling `evolve()` on either result returns `ParseInt` again, since this stage always supplies itself as the
continuation:

```scala mdoc
okResult.evolve()
errResult.evolve()
```

## Disposing Resources

`dispose()` releases all resources held by the stage that produced this `Evolution`.  
After `dispose()` is called the producing stage must be considered permanently unusable — it must not be
applied or skipped again.

Whoever terminates a pipeline must dispose the evolution of the final `Yield`, so that resources are released
immediately rather than carried forward. The reference terminal driver — the `execute` extension method from
[`h8io.stages.base`](../../lib/base/Outcome.md) — invokes `dispose()` immediately after the stage runs, since
`execute` is a terminal operation and the continuation will never be needed.

By default at most one of `evolve` and `dispose()` is called on any evolution instance: `execute` only disposes,
and a pipeline that continues only evolves, dropping the previous evolution. The two calls are not mutually
exclusive, though: a caller that has obtained the continuation via `evolve` may still call `dispose()` on the same
instance later. Operators that own their inner stage — [`Loop`](../../lib/cycles/Loop.md),
[`Repeat`](../../lib/cycles/Repeat.md) and the rest of the [`cycles`](../../lib/cycles/Overview.md) family — do
exactly that: they evolve the inner evolution eagerly and keep its `dispose` as the terminal cleanup handle for the
generation just constructed. `dispose()` must therefore stay valid after `evolve` and release everything still
alive — including resources acquired while constructing the continuation. `evolve` transfers no ownership: the evolution remains the cleanup point for its lineage until the
continuation has run and produced an evolution of its own, which takes over as the terminal handle from that moment
on.

Exception handling is not part of the core model. A stage whose `apply` throws produces no `Yield` and therefore no
`Evolution` — there is nothing the caller could dispose. Such a stage must release its own resources before letting
the exception escape (see the [Lifecycle](Stage.md#the-lifecycle-apply-and-skip) section).

Implementations that hold no external resources may leave `dispose()` as a no-op.

A stage that acquires a resource at construction time can release it in `dispose()`:

```scala mdoc
class ResourceStage(name: String) extends Stage[String, String, Nothing] {
  private var open = true
  println(s"[$name] acquired")

  private val evolution = new Evolution[String, String, Nothing] {
    override def evolve(status: Status[?]): Stage[String, String, Nothing] =
      ResourceStage.this

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

`execute` triggers `dispose()` automatically after the stage runs:

```scala mdoc
new ResourceStage("conn").execute("hello")
```

When multiple stages are composed into a pipeline, both `Evolution` methods — `evolve` and `dispose()` — are called
in the order opposite to the order in which stages are applied: downstream first, then upstream.
This ensures that a downstream stage can still access anything the upstream stage provides right up until the moment
the upstream stage is torn down:

```scala mdoc
val pipeline = new ResourceStage("upstream") ~> new ResourceStage("downstream")
pipeline.execute("hello")
```

## Composing Evolutions

When `~>` composes two stages, it also composes their `Evolution` values using `compose`. The result is an
evolution whose continuation for any status `s` is the sequential composition of the corresponding continuations of
both evolutions:

```
composed(s) == self(s) ~> that(s)
```

The composed evolution calls both halves in the same order: pipeline-downstream first, then pipeline-upstream — the
reverse of the order in which stages are applied.
This applies equally to `evolve` and `dispose()`, since both may release or transition resources held by the
producing stage.  
The [Diagram](../Diagram.md) section on finalization walks through a concrete example of why this matters.

`compose` is an implementation detail of composition and is not part of the typical application-level API; the
evolution classes it builds are internal to the core.

## Adapting Stages with map

`map` produces a new `Evolution` whose continuation is the result of applying a function to the stage the original
evolution would have returned.  
Disposal is delegated unchanged to the wrapped evolution.

This is how an operator keeps its wrapping in place across generations: a decorator such as
[`Lift`](../../lib/operators/Lift.md), [`Safe`](../../lib/operators/Safe.md) or
[`CompleteIfNone`](../../lib/operators/CompleteIfNone.md) maps the inner evolution with its own constructor, so the
continuation comes back wrapped exactly the way the current stage is. The core never calls `map` itself — it is the
tool the lib module builds its decorators on.

This is not what happens when a `Yield.None` propagates through a composed stage. There the downstream stage cannot
be invoked immediately, so it is skipped instead, and the evolution its `skip()` returns is folded in with `compose`
— so that the whole composed pipeline is applied when the next input arrives.
