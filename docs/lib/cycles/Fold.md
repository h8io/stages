# Fold

`Fold` drives a whole cycle of the inner stage on the same outer input — like [`Repeat`](Repeat.md) and
[`Reduce`](Reduce.md) — folding every output the inner stage produces into an accumulator `R` with the binary
operation stage `op`.

Unlike `Reduce`, the accumulator is never empty: `apply` takes the seed `R` alongside the input, so every output —
including the first — is folded via `op((accumulator, output))`; there is no seedless first output to special-case.
Because the accumulator always has a value to yield, `Fold` mixes in [`Fruitful`](../base/Fruitful.md): `apply` is
statically guaranteed to return [`Yield.Some`](../../core/classes/Yield.md).

That guarantee shapes what happens when `op` itself produces no output for a given fold — for example, it filters
some outputs out: the accumulator is left unchanged rather than discarded. `Reduce` can thread an unfolded value
onward as `None`, since its accumulator is optional to begin with; `Fold` cannot, since a `Fruitful` stage has no
`None` to fall back to. `op`'s status is still combined into the iteration's status in this case, since `op` was
applied (not skipped) for that output. When the inner stage itself produces no output, `op` is skipped instead — as
in `Reduce` — and only the inner stage's status carries the iteration.

The cycle stops when the status of an iteration is [`Status.Complete`](../../core/classes/Status.md): an error-free
`Complete` is reported to the enclosing pipeline as `Success`, a `Complete` with errors is preserved. The
accumulator as it stands at that point is yielded.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.cycles.*
```

```scala mdoc
final class UpTo(i: Int, limit: Int) extends SAMStage[Unit, Int, Nothing] {
  override def apply(in: Unit): Yield[Unit, Int, Nothing] =
    if (i >= limit) Yield.Some(i, Status.complete, this)
    else             Yield.Some(i, Status.Success, new UpTo(i + 1, limit).toEvolution)
}

object SumEvens extends StaticStage[(Int, Int), Int, Nothing] {
  override protected def process(in: (Int, Int)): StaticYield[Int, Nothing] = {
    val (acc, out) = in
    if (out % 2 == 0) StaticYield.Some(acc + out, Status.Success)
    else              StaticYield.None(Status.Success)
  }
}

Fold(new UpTo(1, 3), SumEvens)((0, ()))
```

`new UpTo(1, 3)` emits `1`, `2`, then `3` with `Complete`. `SumEvens` only folds even outputs, so `1` and `3` leave the
accumulator untouched and only `2` is added: `Fold` absorbs the error-free `Complete` and returns
`Some(2, Success, ...)`, ready for the next invocation with whatever seed it is given.
