# Overview

The `stages-cats` module integrates the `stages` library with the [cats](https://typelevel.org/cats/) ecosystem.
It depends on `cats-core` and provides three things:

- [`IOr`](classes/IOr.md) — a binary operator that combines stage outputs into `cats.data.Ior`, covering all four
  combinations of left/right presence, including the case where both sides produce a value simultaneously.
- [`Validated`](classes/Validated.md) — projections for `cats.data.Validated` that route `Valid` and `Invalid` values
  into the pipeline independently.
- [`StatusInstances`](classes/StatusInstances.md) — `cats.Monoid` and `cats.Eq` instances for `Status`, exposing the
  algebraic structure described in the `core` module as standard cats typeclasses.
