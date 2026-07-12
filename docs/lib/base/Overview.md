# Overview

The `h8io.stages.base` package sits between the core abstractions and concrete stage implementations.
The core [`Stage`](../../core/classes/Stage.md) interface — implement `apply` and `skip`, supply an `Evolution`
with `evolve` and `dispose` — is intentionally minimal. That minimalism is useful at the boundary, but it leaves
every implementor to repeat the same structural boilerplate: a stage that never changes needs the same
`skip`-returns-`this` pattern; a stage that wraps another needs to forward `dispose`; a stage backed by a pure
function always writes the same `Yield.Some(f(in), Status.Success, this)`.

The `base` package captures those patterns as composable, reusable traits. Concrete stages mix in what they need and
implement only the logic that is genuinely specific to them.

The package also hosts the terminal side of the model: the `execute` extension method — the reference one-shot
driver that runs a stage once, disposes the evolution, and returns a plain result — together with its result type
[`Outcome`](Outcome.md).

## The Immutability Hierarchy

The most common class of stages is those that do not change between runs: a filter, a transformer, a pure mapping.
None of these need to carry state from one invocation to the next; their evolution always returns the same stage.
The `base` package captures this with a small hierarchy, each layer sealing more boilerplate.
