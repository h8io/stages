# Swap

`Swap` is a polymorphic singleton: a single [`Fn`](../base/Fn.md)`[(Any, Any), (Any, Any)]` instance that swaps the
two elements of a pair, always yielding [`Status.Success`](../../core/classes/Status.md). Use `Swap[L, R]` to
obtain a typed `Fn[(L, R), (R, L)]`.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val stage = Swap[String, Int]
stage(("a", 1))
```

[`Reduce`](../cycles/Reduce.md) and [`Fold`](../cycles/Fold.md) both call their `op` in the "left" convention,
`op((accumulator, output))`. Composing `Swap[R, O] ~> op` in front of an `op` written the other way around —
`(output, accumulator)` — adapts it without needing a separate `ReduceRight`/`FoldRight` operator. For a
non-commutative `op` this changes the result:

```scala mdoc
import h8io.stages.base.*
import h8io.stages.cycles.*

object Once extends SAMStage[Unit, Int, Nothing] {
  override def apply(in: Unit): Yield[Unit, Int, Nothing] = Yield.Some(3, Status.complete, this)
}

object Sub extends Fn[(Int, Int), Int] {
  override protected def f(in: (Int, Int)): Int = in._1 - in._2
}

Fold(Once, Sub)((10, ()))                   // "left": accumulator - output
Fold(Once, Swap[Int, Int] ~> Sub)((10, ())) // "right": output - accumulator, via Swap
```
