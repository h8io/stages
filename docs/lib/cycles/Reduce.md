# Reduce

`Reduce` drives a whole cycle of the inner stage on the same outer input — like [`Repeat`](Repeat.md) — folding
every output the inner stage produces into an accumulator with the binary operation stage `op`.

The first output becomes the accumulator without invoking `op` (the classic seedless reduce); each subsequent
output is folded via `op((accumulator, output))`. In an iteration with nothing to fold — the inner stage produced
no output, or the output is the first one — `op` is skipped, so each iteration performs exactly one of
`apply`/`skip` on both inner stages, as the lifecycle contract requires. An iteration yielding no output with
[`Status.Success`](../../core/classes/Status.md) keeps the cycle spinning, like `Repeat`.

When `op` is applied but itself yields no output for a given fold — for example, it filters some outputs out —
the accumulator is left unchanged rather than discarded, exactly as in [`Fold`](Fold.md). `op`'s status is still
combined into the iteration's status in this case, since `op` was applied, not skipped, for that output.

The cycle stops when the status of an iteration is [`Status.Complete`](../../core/classes/Status.md): an
error-free `Complete` is reported to the enclosing pipeline as `Success`, a `Complete` with errors is preserved.
The value accumulated so far, if any, is yielded. The accumulator is local to one outer run: the next generation
of `Reduce` starts empty.

The inner stage and `op` together form the inner pipeline of `Reduce`, with `op` downstream. An iteration's
status is the combination of their statuses in pipeline order (the inner stage first), and both continuations are
selected by that combined status — exactly as a composed pipeline evolves both sides with the status of the whole
run. Otherwise the status isolation of the family holds as in [`Loop`](Loop.md) and `Repeat`: the continuations
are never selected on the status `Reduce` reports outwards, and a skipped `Reduce` skips both inner stages and
selects their continuations on the neutral `Success`.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.std.*
import h8io.stages.cycles.*
```

```scala mdoc
object Sum extends Fn[(Int, Int), Int] {
  override protected def f(in: (Int, Int)): Int = in._1 + in._2
}

Reduce(Countdown[Int](3), Sum)(5)
```

[`Countdown(3)`](../std/Countdown.md) emits the input `5` three times and signals `Complete` on the third. The
first `5` seeds the accumulator, the other two are folded via `Sum`, so `Reduce` absorbs the error-free
`Complete` and returns `Some(15, Success, ...)`, ready for the next invocation.

```scala mdoc
final class UpTo(i: Int, limit: Int) extends SAMStage[Unit, Int, Nothing] {
  override def apply(in: Unit): Yield[Unit, Int, Nothing] =
    if (i >= limit) Yield.Some(i, Status.complete, this)
    else             Yield.Some(i, Status.Success, new UpTo(i + 1, limit).toEvolution)
}

object SumOnlyEven extends StaticStage[(Int, Int), Int, Nothing] {
  override protected def process(in: (Int, Int)): StaticYield[Int, Nothing] = {
    val (acc, out) = in
    if (out % 2 == 0) StaticYield.Some(acc + out, Status.Success)
    else              StaticYield.None(Status.Success)
  }
}

Reduce(new UpTo(1, 5), SumOnlyEven)(())
```

`new UpTo(1, 5)` emits `1`, `2`, `3`, `4`, then `5` with `Complete`. The first output, `1`, seeds the accumulator.
`SumOnlyEven` only folds even outputs, so `3` and the completing `5` leave the accumulator untouched instead of
resetting it, and only `2` and `4` are added: `Reduce` returns `Some(7, Success, ...)`.
