# FruitfulStaticStage

`FruitfulStaticStage` is the fruitful counterpart of [`StaticStage`](StaticStage.md): a stage that is its own
evolution *and* always produces an output value. `apply` and `skip` are sealed; the single abstract method is
`process(in: I): (O, Status[E])` — the output value paired with its status, no yield wrapping at all.

`apply` carries the `Yield.Some` return type required by [`Fruitful`](Fruitful.md), so the always-an-output
guarantee is tracked statically.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

```scala mdoc
object ClampNonNegative extends FruitfulStaticStage[Int, Int, String] {
  override protected def process(in: Int): (Int, Status[String]) =
    if (in >= 0) (in, Status.Success) else (0, Status.error(s"negative input: $in"))
}

ClampNonNegative(7)
ClampNonNegative(-3)
```

Use `FruitfulStaticStage` when the stage always has a value to return but the status may vary — for example
[`Complete`](../std/Complete.md), [`GlobalSoftDeadline`](../std/GlobalSoftDeadline.md), and the
[`Tuple2`](../projections/Tuple2.md) projections. When the status is always `Success` as well, [`Fn`](Fn.md) is
more specific; when output is not always produced, use [`StaticStage`](StaticStage.md).
