# Overview

The `h8io.stages.cycles` package hosts operators that drive a whole _cycle_ of the inner stage within a single
outer run: on every `apply` they keep re-applying the inner stage's generations — tail-recursively, without
growing the stack — until the stage signals the end of the cycle with
[`Status.Complete`](../../core/classes/Status.md).

[`Loop`](Loop.md) feeds each output back as the next input; [`Repeat`](Repeat.md) re-applies the stage to the
same input. Both absorb an error-free `Complete` into a `Success` and preserve accumulated errors.

The family shares one lifecycle contract:

- **Status isolation.** The continuation of the inner stage is always selected on the status the inner stage
  itself produced — never on the status the operator reports outwards, and never on the status the enclosing
  pipeline later evolves the operator with. The evolution returned outwards is a
  [`ConstEvolution`](../base/ConstEvolution.md) — exactly the barrier for the outer status. A skipped operator
  skips its inner stage and selects the inner continuation on the neutral `Success`.
- **Lifecycle ownership.** The operator fully owns the inner stage: it evolves the inner evolution eagerly and
  keeps its `dispose` as the terminal cleanup handle for the generation just constructed — see the
  [`Evolution`](../../core/classes/Evolution.md) contract.
