# Overview

The `lib` module is the standard library built on top of the [`core`](../core/Overview.md) algebra: the shapes an
implementor starts a stage from, the operators that combine stages, and the ready-made stages that need no
implementing at all. Like `core`, it brings no dependencies of its own.

It also hosts the terminal side of the model, which `core` deliberately leaves out: the `execute` extension method
and its result type [`Outcome`](base/Outcome.md). Running a pipeline is a policy, not part of its algebra.

## The Packages

- [`base`](base/Overview.md) — the traits stages are built from: the immutability hierarchy
  ([`Stagnation`](base/Stagnation.md) → [`SAMStage`](base/SAMStage.md) → [`StaticStage`](base/StaticStage.md),
  [`FruitfulStaticStage`](base/FruitfulStaticStage.md), [`Fn`](base/Fn.md)), the wrapping shapes
  ([`Alterator`](base/Alterator.md), [`BinaryOperator`](base/BinaryOperator.md)), and the terminal driver.
- [`std`](std/Overview.md) — stages that stand on their own: constants, pass-throughs, counters, deadlines.
- [`operators`](operators/Overview.md) — stages that wrap other stages and change what they do.
- [`cycles`](cycles/Overview.md) — operators that drive a whole cycle of the inner stage within a single outer run.
- [`projections`](projections/Overview.md) — stages that read one value out of a container, yielding nothing when
  it holds the other side.

## Polymorphic Singletons

A stage that holds no value of its own needs only one instance. [`std.Identity`](std/Identity.md),
[`Swap`](std/Swap.md), [`Coalesce`](std/Coalesce.md), [`Complete`](std/Complete.md), the
[projections](projections/Overview.md) and a few [operators](operators/Overview.md) are each a single object
defined over `Any`, with a typed `apply[T]` that casts it to what the use site needs: `Identity[String]` is a
`Fn.Endo[String]`, `Either.Left[Int]` a `Projection[Either[Int, Any], Int]`. Nothing is allocated, and the cast
is sound because the instance never depends on its type parameters.

Such an object therefore has two methods named `apply`: the typed view `apply[T]`, which takes no arguments, and the
stage's own `apply(in)`. Scala 2 reads `Identity(x)` as a call of the stage, since the view takes no arguments.
Scala 3 treats the view as a candidate too, reading the call as `Identity[T](x)`, and rejects it as ambiguous:

```
Ambiguous overload. The overloaded alternatives of method apply in trait LeftProjection with types
 [T]: h8io.stages.base.Projection[Either[T, Any], T]
 (in: Either[Any, Any]): h8io.stages.Yield[Either[Any, Any], Any, Nothing]
both match arguments (Left[Int, Nothing])
```

Both readings would do the same thing, so the cure is to name the type: `Identity[Int](x)`,
`Either.Left[Int](Left(1))`. Only a direct call of the untyped object is affected — composing it with `~>`, running
it with `execute`, or passing it where a stage is expected all work unchanged. The singletons this concerns are
`std.Identity`, `Swap`, `Coalesce` and every projection, including `IOr` and `Validated` in
[`stages-cats`](../cats/Overview.md).

## Where a New Stage Belongs

A stage that does its own work and answers only to its input belongs in `std`. A stage that wraps another and
alters its behaviour is an operator — and if it re-applies that inner stage until it signals the end of a unit of
work, it belongs in `cycles` rather than `operators`. A stage that only reads a value out of a container is a
projection, and gets the `some`/`none` helpers of [`Projection`](base/Projection.md) for free.
