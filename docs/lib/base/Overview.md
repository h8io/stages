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

[`Stagnation`](Stagnation.md) is the root of it. It is an [`Evolution`](../../core/classes/Evolution.md) with a
self-type of `Stage`: mix it into a stage and the stage becomes its own evolution, with `evolve` sealed to return
`this` for every status and `dispose` defaulting to a no-op — still open, so a stage with resources can release
them.

[`SAMStage`](SAMStage.md) adds the other half of the pattern: `skip` is sealed to return `this` as well, so the
stage supplies the same continuation whether it ran or was bypassed. What is left abstract is `apply` alone — hence
the name.

From there the hierarchy branches by how much of the [`Yield`](../../core/classes/Yield.md) the implementor still
has to build. These three are siblings, not layers on one another: each seals `apply` in its own way, and each is
a `SAMStage`.

| Trait | Implement | Yields |
|---|---|---|
| [`StaticStage`](StaticStage.md) | `process(in): StaticYield[O, E]` | a value or nothing, any status |
| [`FruitfulStaticStage`](FruitfulStaticStage.md) | `process(in): (O, Status[E])` | always a value, any status |
| [`Fn`](Fn.md) | `f(in): O` | always a value, always `Status.Success` |

`StaticYield` is a [`Yield`](../../core/classes/Yield.md) without the evolution field, which the sealed `apply`
supplies as `this`. The two fruitful traits also mix in [`Fruitful`](Fruitful.md), which narrows the return type of
`apply` to `Yield.Some` — so "always a value" is a guarantee the compiler tracks, not just a convention.

## The Rest of the Package

Two more base traits describe shapes rather than immutability: [`Alterator`](Alterator.md) for a stage wrapping one
inner stage (with the `UnaryOperator`, `Decorator`, `Alteration` and `Decoration` aliases built around it), and
[`BinaryOperator`](BinaryOperator.md) for a stage combining two of them on a shared input.

[`SafeStage`](SafeStage.md) splits `apply` into a happy path and a recovery path;
[`Projection`](Projection.md) is the shape shared by the stages that read one value out of a container;
[`ConstEvolution`](ConstEvolution.md), together with the `toEvolution` extension, lifts a stage into an evolution
that always returns it.
