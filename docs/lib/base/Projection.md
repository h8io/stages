# Projection

`Projection` is a [`StaticStage`](StaticStage.md) for extracting a value from a container type. When the expected
value is present it returns [`Yield.Some`](../../core/classes/Yield.md); when it is absent it returns `Yield.None`
with [`Status.Success`](../../core/classes/Status.md), preserving the overall pipeline status without signalling an
error.

Two convenience members simplify `process` implementations: `some(out)` produces `StaticYield.Some(out, Status.Success)`
and `none` is a pre-built `StaticYield.None(Status.Success)` — both with `this` as the implicit evolution.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

```scala mdoc
object OptionGet extends Projection[Option[Int], Int] {
  override protected def process(in: Option[Int]): StaticYield[Int, Nothing] =
    in match {
      case Some(v) => some(v)
      case None    => none
    }
}

OptionGet(Some(7))
OptionGet(None)
```

For covariant binary type constructors `C[+_, +_]`, `LeftProjection[C]` and `RightProjection[C]` provide reusable
singleton projections. Each is defined over the widened type `C[Any, ?]` or `C[?, Any]` and exposed as a concrete
`Projection[C[T, ?], T]` via a typed `apply[T]` method. The cast is sound because `C` is covariant in both
positions, so a `C[Any, ?]` can be read safely as a `C[T, ?]` for any `T`.
