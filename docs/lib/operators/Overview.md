# Overview

The `h8io.stages.operators` package holds the stages that wrap other stages. Each applies its inner stage once per
run — for the operators that drive a whole cycle of it instead, see [`cycles`](../cycles/Overview.md).

## Decorators

A decorator wraps one stage and keeps its type: an [`Alterator`](../base/Alterator.md) whose evolution is mapped
so that the wrapping survives into the next generation.

| Operator | What it changes |
|---|---|
| [`CompleteIfNone`](CompleteIfNone.md) | turns a `Yield.None` with `Success` into one with `Status.Complete` |
| [`KeepLastOutput`](KeepLastOutput.md) | re-emits the last output when the inner stage yields none |
| [`Lift`](Lift.md) | wraps the optional output into an `Option`, so a value is always produced |
| [`Safe`](Safe.md) | catches non-fatal exceptions and reports them as errors |
| [`LocalSoftDeadline`](LocalSoftDeadline.md) | completes the run once the current unit of work is out of time |

## Binary Operators

These take two stages over a shared input and combine what both produced — see
[`BinaryOperator`](../base/BinaryOperator.md) for the shape they share.

| Operator | Output | Right side |
|---|---|---|
| [`And`](And.md) | `(LO, RO)` | skipped when the left yields nothing |
| [`IAnd`](IAnd.md) | `(LO, RO)` | always applied |
| [`Or`](Or.md) | `Either[LO, RO]` | applied only when the left yields nothing |

The inclusive-or of the family, `IOr`, lives in the [`stages-cats`](../../cats/classes/IOr.md) module, since its
output type is `cats.data.Ior`.

## Decorations

[`Identity`](Identity.md) and [`CompleteIfSome`](CompleteIfSome.md) are not stages but
[`Decoration`](../base/Alterator.md)s — functions from a stage to a stage. `Identity` returns it untouched;
`CompleteIfSome` appends [`Complete`](../std/Complete.md) after it with `~>`. Both are singletons with a typed
`apply[I, O, E]`, so they can be passed wherever a decoration is expected without allocating one.
