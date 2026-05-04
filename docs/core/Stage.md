# Stage

`Stage` is the fundamental processing unit of a pipeline.  
It is a function `I => Yield[I, O, E]` that transforms a single input value into a [`Yield`](Yield.md) — an
optional output, a [`Status`](Status.md), and an [`Evolution`](Evolution.md) that decides what the pipeline looks
like for the next run.

Stages are contravariant in their input type `I` and covariant in their output type `O` and error type `E`.
This variance enables safe composition: a stage that accepts a wider input type can be used wherever a more
specific one is expected, and outputs can flow naturally from one stage to the next.

```scala mdoc
import h8io.stages.*
```

## The Lifecycle: apply and skip

Every `Stage` participates in exactly one of two paths during a pipeline run.

**apply** — the active path. The stage receives a real input value and returns a `Yield`. It is free to perform
side effects, update internal state, and produce any output it likes.

**skip** — the bypassed path. The stage is not given any input. Instead it returns the `Evolution` it _would_ have
returned had it run — without performing any side effects or consuming any value. This happens when an upstream stage
produces `Yield.None` and there is nothing to pass downstream, or when a non-inclusive binary operation excludes
this branch of the pipeline.

`skip` exists precisely so that stages further downstream still get a chance to evolve correctly even when they
are not directly executed. The [Diagram](Diagram.md) explains this with a concrete example involving Stage 3-1,
which is skipped in the first generation but still needs to evolve into Stage 3-2.

Here is a minimal stage that doubles its input on the active path and supplies its evolution on the skipped path:

```scala mdoc
object Double extends Stage[Int, Int, Nothing] {
  private def evo: Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def apply(status: Status[?]): Stage[Int, Int, Nothing] = Double
    override def dispose(): Unit = ()
  }

  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(in * 2, Status.Success, evo)

  override def skip(): Evolution[Int, Int, Nothing] = evo
}

Double(7)
Double.skip()
```

## Building Pipelines with ~>

`~>` composes two stages into a `Stage.AndThen`: the output of the left stage becomes the input of the right stage.
The result is itself a `Stage`, so further stages can be appended with additional `~>` calls:

```scala mdoc
object ToString extends Stage[Int, String, Nothing] {
  private def evo: Evolution[Int, String, Nothing] = new Evolution[Int, String, Nothing] {
    override def apply(status: Status[?]): Stage[Int, String, Nothing] = ToString
    override def dispose(): Unit = ()
  }

  override def apply(in: Int): Yield[Int, String, Nothing] =
    Yield.Some(in.toString, Status.Success, evo)

  override def skip(): Evolution[Int, String, Nothing] = evo
}

object Shout extends Stage[String, String, Nothing] {
  private def evo: Evolution[String, String, Nothing] = new Evolution[String, String, Nothing] {
    override def apply(status: Status[?]): Stage[String, String, Nothing] = Shout
    override def dispose(): Unit = ()
  }

  override def apply(in: String): Yield[String, String, Nothing] =
    Yield.Some(in.toUpperCase + "!", Status.Success, evo)

  override def skip(): Evolution[String, String, Nothing] = evo
}

val pipeline = Double ~> ToString ~> Shout
```

The static type of `pipeline` is `Stage[Int, String, Nothing]`: it accepts the input of the first stage and
produces the output of the last one. The intermediate type `Int → String` is erased at the pipeline boundary.

Running the pipeline produces a single `Yield` that combines the status and evolution of all three stages:

```scala mdoc
pipeline(5)
```

When the upstream stage produces `Yield.None`, the downstream stage is not applied; instead it is wired into the
combined evolution so that it will be called when the next input arrives.

## Terminal Execution with execute

`execute` is the one-shot path: it applies the stage to an input, immediately disposes the `Evolution`, and returns
a plain [`Outcome`](Outcome.md) with no continuation.

It is the right choice when a single stage — or a composed pipeline — should run exactly once and the next
generation is not needed:

```scala mdoc
val outcome = pipeline.execute(5)
```

The disposal of the `Evolution` happens inside `execute` regardless of what the stage returned. If `dispose()`
throws a non-fatal exception, the exception is captured in `Outcome.disposeFailure` and the outcome is still
returned normally. Fatal exceptions are not caught and will propagate.
