# Fn

`Fn` is the simplest way to turn a single pure function into a [`Stage`](../../core/classes/Stage.md): extend `Fn`
and implement `f`. Everything else — wrapping in `Yield.Some`, attaching `Status.Success`, wiring the evolution —
is handled automatically.

As the most constrained member of the hierarchy, a `Fn` stage can never produce `Yield.None` or a non-`Success`
status. When output is guaranteed but the status may vary, use [`FruitfulStaticStage`](FruitfulStaticStage.md);
when output is not always produced, use [`StaticStage`](StaticStage.md).

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
