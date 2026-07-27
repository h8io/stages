# Scan

`Scan` folds its own inputs into a running accumulator across pipeline runs, via the binary operation stage `op` —
the running-total counterpart of [`Reduce`](../cycles/Reduce.md) and [`Fold`](../cycles/Fold.md). Unlike that
`cycles` family, `Scan` has no inner stage and no cycle: it consumes one external input per outer run and threads
its own accumulator from one run to the next.

The first input becomes the accumulator unchanged, without invoking `op`; each subsequent input is folded via
`op((accumulator, input))`, and the result — the new running total — is emitted immediately. `op` is a
`Stage.Fruitful`, so every fold yields the next running total and `Scan` itself is fruitful: every run emits a
value.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.std.*
```

```scala mdoc
object Sum extends Fn[(Int, Int), Int] {
  override protected def f(in: (Int, Int)): Int = in._1 + in._2
}

val running = Scan(Sum)
val y1 = running(1)
val y2 = y1.evolve()(2)
val y3 = y2.evolve()(3)
```

`1` seeds the accumulator; `2` and `3` are folded in, so each run reports the running total: `1`, `3`, `6`.

Filtering is expressed by returning the accumulator unchanged rather than by yielding nothing, exactly as in
`Reduce`/`Fold` — a declining fold has nothing else to hand back:

```scala mdoc
object SumOnlyEven extends Fn[(Int, Int), Int] {
  override protected def f(in: (Int, Int)): Int = {
    val (acc, out) = in
    if (out % 2 == 0) acc + out else acc
  }
}

val filtered = Scan(SumOnlyEven)
val z1 = filtered(10)
val z2 = z1.evolve()(3)
val z3 = z2.evolve()(4)
```

`10` seeds the accumulator. `3` is odd, so `SumOnlyEven` hands the accumulator back untouched and `z2` re-emits
`10`. `4` is even, so it folds against the still-`10` accumulator: `z3` is `Some(14, ...)`.

Whenever a run's status is [`Status.Complete`](../../core/classes/Status.md), the next generation resets to a fresh,
unseeded `Scan` instead of continuing to accumulate — mirroring how [`Countdown`](Countdown.md) resets on any
non-`Success` status. `Complete` marks one logical unit of work as finished, so the next input seeds a new one from
scratch rather than folding into whatever was accumulated before:

```scala mdoc
final class SumUntil(limit: Int) extends FruitfulSAMStage[(Int, Int), Int, Nothing] {
  override def apply(in: (Int, Int)): Yield.Some.Fruitful[(Int, Int), Int, Nothing] = {
    val (acc, out) = in
    val sum = acc + out
    val status: Status[Nothing] = if (sum >= limit) Status.complete else Status.Success
    Yield.Some.Fruitful(sum, status, this)
  }
}

val bounded = Scan(new SumUntil(10))
val w1 = bounded(4)
val w2 = w1.evolve()(3)
val w3 = w2.evolve()(9) // 4 + 3 = 7, then 7 + 9 = 16 >= 10, so Complete
val w4 = w3.evolve()(2) // resets: seeds fresh with 2 instead of folding 16 + 2
```

`w3` reaches `16` and signals `Complete`, so `w4` does not fold `2` into it: it seeds a brand new accumulator,
`Some(2, Success, ...)`.
