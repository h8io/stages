# Fn

`Fn` is the simplest way to turn a single pure function into a [`Stage`](../../core/classes/Stage.md): extend `Fn`
and implement `f`. Everything else — wrapping in `Yield.Some`, attaching `Status.Success`, wiring the evolution —
is handled automatically.

As the most constrained member of the hierarchy, a `Fn` stage can never produce `Yield.None` or a non-`Success`
status — and neither can any generation it evolves into, since that generation is this same instance. It is
therefore a [`Stage.Fruitful`](../../core/classes/Stage.md), via [`FruitfulSAMStage`](FruitfulSAMStage.md): `apply`
returns `Yield.Some.Fruitful`, and composing two `Fn` stages with `~>` gives back a fruitful stage.

When output is guaranteed but the status may vary, use [`FruitfulStaticStage`](FruitfulStaticStage.md); when output
is not always produced, use [`StaticStage`](StaticStage.md).

```scala mdoc
import h8io.stages.base.*
```

```scala mdoc
object DoubleInt extends Fn[Int, Int] {
  override protected def f(in: Int): Int = in * 2
}

DoubleInt(21)
DoubleInt.skip()
```

`Fn` also defines `Fn.Endo[T]` as a type alias for endomorphic stages that map a type to itself.

Because a `Fn` is fruitful, it satisfies the `op` requirement of [`Fold`](../cycles/Fold.md),
[`Reduce`](../cycles/Reduce.md) and [`Scan`](../std/Scan.md) without further ceremony — a fold has to yield the next
accumulator every time, and a pure function always does.
