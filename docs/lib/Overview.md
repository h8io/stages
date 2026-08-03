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

## Where a New Stage Belongs

A stage that does its own work and answers only to its input belongs in `std`. A stage that wraps another and
alters its behaviour is an operator — and if it re-applies that inner stage until it signals the end of a unit of
work, it belongs in `cycles` rather than `operators`. A stage that only reads a value out of a container is a
projection, and gets the `some`/`none` helpers of [`Projection`](base/Projection.md) for free.
