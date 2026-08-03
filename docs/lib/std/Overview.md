# Overview

The `h8io.stages.std` package holds the stages that stand on their own: they answer to their input, and none of
them wraps another stage whose behaviour they alter. Most are built from the
[immutability hierarchy](../base/Overview.md#the-immutability-hierarchy), so they carry no state between runs
unless their whole purpose is to carry some.

| Stage | What it does |
|---|---|
| [`Const`](Const.md) | ignores its input, always yields the same value |
| [`Identity`](Identity.md) | passes its input through unchanged |
| [`Swap`](Swap.md) | swaps the two elements of a pair |
| [`Coalesce`](Coalesce.md) | collapses an `Either[T, T]` to the value either side holds |
| [`Complete`](Complete.md) | passes the input through and signals the end of the unit of work |
| [`DeadEnd`](DeadEnd.md) | yields nothing, with `Status.Complete` — the end-of-stream marker |
| [`Countdown`](Countdown.md) | passes `n` values through, then completes and resets |
| [`GlobalSoftDeadline`](GlobalSoftDeadline.md) | passes values through until a deadline fixed at construction |
| [`Scan`](Scan.md) | folds its own inputs into a running accumulator across runs |

The first four never fail and never change: they are [`Fn`](../base/Fn.md) stages. Four of them —
[`Identity`](Identity.md), [`Swap`](Swap.md), [`Coalesce`](Coalesce.md) and [`Complete`](Complete.md) — hold no
value at all, so each is a single instance defined over `Any` and cast to what the use site needs by a typed
`apply[T]`, rather than allocated afresh.

The last two carry something between runs. [`Countdown`](Countdown.md) counts, and
[`Scan`](Scan.md) accumulates — both by evolving into the next generation of themselves rather than by mutating,
and both resetting on any status that is not `Success`. [`GlobalSoftDeadline`](GlobalSoftDeadline.md) sits between:
it never evolves, but the clock it reads moves on its own.
