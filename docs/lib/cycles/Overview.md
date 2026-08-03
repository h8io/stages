# Overview

The `h8io.stages.cycles` package hosts operators that drive a whole _cycle_ of the inner stage within a single
outer run: on every `apply` they keep re-applying the inner stage's generations — tail-recursively, without
growing the stack — until the stage signals the end of the cycle with
[`Status.Complete`](../../core/classes/Status.md).

[`Loop`](Loop.md) feeds each output back as the next input; [`Repeat`](Repeat.md) re-applies the stage to the
same input; [`Reduce`](Reduce.md) re-applies the stage to the same input like `Repeat` and folds every output
into an accumulator with a binary operation stage; [`Fold`](Fold.md) does the same as `Reduce`, but seeded from
the outer input, so — unlike `Reduce` — it always has a value to yield. All of them absorb an error-free
`Complete` into a `Success` and preserve accumulated errors.

Feeding the output back gives `Loop` one more way out of the cycle: an iteration that produces no output leaves it
with nothing to feed, so the cycle ends there too. For the other three the status alone decides — an iteration
yielding nothing with `Success` keeps them spinning.

The family shares one lifecycle contract:

- **Status isolation.** The continuation of the inner stage is always selected on the status the inner stage
  itself produced — never on the status the operator reports outwards, and never on the status the enclosing
  pipeline later evolves the operator with. The evolution returned outwards is a
  [`ConstEvolution`](../base/ConstEvolution.md) — exactly the barrier for the outer status. A skipped operator
  skips its inner stage and selects the inner continuation on the neutral `Success`.
- **Lifecycle ownership.** The operator fully owns its inner stages: it evolves the inner evolutions eagerly and
  keeps their `dispose` as the terminal cleanup handle for the generation just constructed — see the
  [`Evolution`](../../core/classes/Evolution.md) contract.
