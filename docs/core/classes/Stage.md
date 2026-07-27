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

**skip** — the bypassed path. Any stage that participates in a pipeline run but does not process the current input
must take this path: it returns the `Evolution` it _would_ have returned had it run, without consuming any value.
Like `apply`, it may perform side effects — a decorator, for example, may advance the lifecycle of the inner stage
it owns. Common triggers are an upstream stage producing `Yield.None` (nothing to pass downstream) or a
non-inclusive binary operation that excludes this branch.

`skip` exists precisely so that stages further downstream still get a chance to evolve correctly even when they
are not directly executed. The [Diagram](../Diagram.md) explains this with a concrete example involving Stage 3-1,
which is skipped in the first generation but still needs to evolve into Stage 3-2.

**apply that throws** — the failure path. No `Yield` — and therefore no [`Evolution`](Evolution.md) — is produced,
so nobody can release the stage's resources from the outside: a stage that throws from `apply` must release its own
resources before the exception escapes, and is permanently unusable afterwards. The core takes no other part in
exception handling; for the simplest — stateless — stages the lib module offers the
[`Safe`](../../lib/operators/Safe.md) crutch.

Here is a minimal stage that doubles its input on the active path and supplies its evolution on the skipped path:

```scala mdoc
object Double extends Stage[Int, Int, Nothing] {
  private def evo: Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def evolve(status: Status[?]): Stage[Int, Int, Nothing] = Double
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

`~>` composes two stages: the output of the left stage becomes the input of the right stage. The composed node
itself is internal to the core — what you get back is a `Stage`, so further stages can be appended with additional
`~>` calls:

```scala mdoc
object ToString extends Stage[Int, String, Nothing] {
  private def evo: Evolution[Int, String, Nothing] = new Evolution[Int, String, Nothing] {
    override def evolve(status: Status[?]): Stage[Int, String, Nothing] = ToString
    override def dispose(): Unit = ()
  }

  override def apply(in: Int): Yield[Int, String, Nothing] =
    Yield.Some(in.toString, Status.Success, evo)

  override def skip(): Evolution[Int, String, Nothing] = evo
}

object Shout extends Stage[String, String, Nothing] {
  private def evo: Evolution[String, String, Nothing] = new Evolution[String, String, Nothing] {
    override def evolve(status: Status[?]): Stage[String, String, Nothing] = Shout
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

## Always an Output: Stage.Fruitful

Some stages produce an output for every input they are given. `Stage.Fruitful` is the refinement that says so in
the type: `apply` returns [`Yield.Some.Fruitful`](Yield.md) rather than the broader `Yield`, and `skip` returns an
`Evolution.Fruitful`.

The second half is what makes the guarantee worth having. A stage that promises an output only for the current run
loses the promise the moment the pipeline evolves — the next generation is typed as a plain `Stage` again.
`Stage.Fruitful` carries the guarantee along the whole lineage: every generation it evolves into is fruitful too.
That closure is not decoration; without it `fruitful ~> fruitful` would stop being fruitful after the first run.

`~>` has an overload for it: composing two fruitful stages gives back a `Stage.Fruitful`, so the guarantee survives
composition. Mixing a fruitful stage with an ordinary one falls back to a plain `Stage` — the composition can yield
nothing whenever either side can.

```scala mdoc
object Triple extends Stage.Fruitful[Int, Int, Nothing] with Evolution.Fruitful[Int, Int, Nothing] {
  override def apply(in: Int): Yield.Some.Fruitful[Int, Int, Nothing] =
    Yield.Some.Fruitful(in * 3, Status.Success, this)

  override def skip(): Evolution.Fruitful[Int, Int, Nothing] = this
  override def evolve(status: Status[?]): Stage.Fruitful[Int, Int, Nothing] = this
  override def dispose(): Unit = ()
}

val fruitful: Stage.Fruitful[Int, Int, Nothing] = Triple ~> Triple
fruitful(2)
fruitful(2).evolve()          // still a Stage.Fruitful
val plain: Stage[Int, Int, Nothing] = Double ~> Triple
```

Implementing the refinement by hand is rarely necessary: the lib module's
[`FruitfulSAMStage`](../../lib/base/FruitfulSAMStage.md) covers the stage that is its own evolution, and
[`Fn`](../../lib/base/Fn.md) covers the pure-function case.

`Stage.Fruitful.Endo[T, E]` is the usual alias for the endomorphic case.

## Terminal Execution

The core model ends at `Yield`: whoever terminates a pipeline is responsible for disposing the evolution of the
final `Yield`. The reference terminal driver is the `execute` extension method from `h8io.stages.base` — the
one-shot path that applies the stage to an input, immediately disposes the `Evolution`, and returns a plain
[`Outcome`](../../lib/base/Outcome.md) with no continuation:

```scala mdoc
import h8io.stages.base.*

val outcome = pipeline.execute(5)
```

See [`Outcome`](../../lib/base/Outcome.md) for the details, including how disposal failures are reported.
