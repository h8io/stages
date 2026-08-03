# Overview

The `core` module defines the minimal algebra on which the rest of the library is built: four types and a single
composition operator. The module carries no library dependencies.

## Motivation

A pipeline library needs two things at its foundation: a type that represents a processing step, and a type that
describes what that step produced. Everything else — utility operators, effect integration, binary combinators — is
layered on top.

Keeping these concerns in a single artefact would force every downstream user to pull in the full dependency tree.
By isolating the minimal algebra, `core` stays embeddable anywhere Scala runs without pulling in cats, scalaz, or
any effect system. `stages-lib` and `stages-cats` depend on `core` and extend it.

## The types

[`Stage`](classes/Stage.md)`[-I, +O, +E]` is a function `I => Yield[I, O, E]`, extended with
`skip(): Evolution[I, O, E]` for the bypass path. Exactly one of `apply` or `skip` is called per pipeline run; the
[`Evolution`](classes/Evolution.md) returned from either call owns resource cleanup for that stage.

[`Yield`](classes/Yield.md)`[-I, +O, +E]` is what a stage returns when applied: an optional output value, a
[`Status`](classes/Status.md), and an [`Evolution`](classes/Evolution.md).
The `I` parameter is contravariant because the enclosed `Evolution` will eventually consume inputs again.

[`Status`](classes/Status.md)`[+E]` is the completion signal attached to every [`Yield`](classes/Yield.md):
`Success` for ordinary processing, or `Complete[E]` when a unit of work has finished — cleanly or with accumulated
errors. `Complete` is passed to [`Evolution`](classes/Evolution.md) when selecting the next continuation stage, and
is intercepted by the enclosing [`cycles`](../lib/cycles/Overview.md) operators — `Loop`, `Repeat`, `Reduce`,
`Fold` — as the cue to end the current cycle.

[`Evolution`](classes/Evolution.md)`[-I, +O, +E]` is a continuation: given a [`Status`](classes/Status.md), it returns
the next stage to use for re-processing. It also owns disposal of the resources held by the stage that produced it.

The core model ends at [`Yield`](classes/Yield.md). The one-shot terminal driver `execute` and its result type
[`Outcome`](../lib/base/Outcome.md)`[+O, +E]` — a `Yield` stripped of its `Evolution` — live in the lib module
(`h8io.stages.base`).

## Algebraic structure

### `Status[E]` is a monoid

`Success` is the identity element; `combine` is the binary operation:

```
Success.combine(s)             == s
s.combine(Success)             == s
Complete(e1).combine(Complete(e2)) == Complete(e1 ++ e2)
```

`Complete` dominates `Success` and two `Complete` values accumulate errors by concatenation.
The monoid is not commutative: error order reflects pipeline order.
`stages-cats` exposes this as a `cats.Monoid[Status[E]]`.

### `Stage.Endo[T, E]` is a monoid

Endomorphic stages — `Stage[T, T, E]` — form a monoid under `~>`:

- **Identity**: the passthrough stage `t => Yield.Some(t, Status.Success, identity_evolution)`.
- **Operation**: sequential composition `s1 ~> s2`.

Associativity follows from the definition of composition: `(a ~> b) ~> c` and `a ~> (b ~> c)` produce the
same observable behaviour for every input. This monoid is verified against the cats `MonoidTests` laws in the test
suite.

### `Evolution.Endo[T, E]` is a monoid

Endomorphic evolutions — `Evolution[T, T, E]` — compose under `Evolution.compose`:

- **Identity**: the evolution of the passthrough stage, which always returns the identity stage.
- **Operation**: `e1.compose(e2)`, whose continuations satisfy `composed(s) == e1(s) ~> e2(s)`.

Disposal always runs in reverse pipeline order — downstream evolutions are disposed first — so that no downstream
stage outlives the upstream resources it depends on.

Stage composition and evolution composition are homomorphic: if `s1 ~> s2` produces stage `p`, then
`s1.skip().compose(s2.skip())` produces the evolution of `p`.

## Kleisli connection

`Stage[-I, +O, +E]` is a Kleisli arrow in the category of Scala types where the effect functor is
`Yield[I, _, E]`:

```
Kleisli[Yield[I, _, E], I, O]  ≅  I => Yield[I, O, E]
```

The `~>` operator is Kleisli composition: for `f: Stage[A, B, E]` and `g: Stage[B, C, E]`, `f ~> g` is the
composed arrow `A => Yield[A, C, E]` that runs `f` then feeds `f`'s output into `g`.

Where stages diverge from plain Kleisli is in the explicit continuation. A standard Kleisli arrow `A => F[B]`
encodes all future behaviour inside `F`. A stage separates the immediate result (`Yield.Some` / `Yield.None` and
`Status`) from the continuation (`Evolution`), making the next generation of the pipeline an observable,
replaceable value rather than an opaque closure. This separation is what allows stages to evolve between runs:
`evolve(status)` selects the next stage based on what happened, rather than having the behaviour fixed at
construction time.

`Yield[I, _, E]` is not a monad in the standard sense — its `I` parameter is contravariant and the evolution
inside it is not compositionally transparent — but the stage category it defines supports the same sequential
reasoning that makes Kleisli arrows useful: stages compose, the composition is associative, and there is an
identity for endomorphisms.
