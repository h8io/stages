# IOr

`IOr` is a binary operator that applies both `left` and `right` to the same input independently and combines
their outputs into a `cats.data.Ior`. Unlike [`And`](../../lib/operators/And.md) (which skips `right` when `left`
yields nothing) and [`Or`](../../lib/operators/Or.md) (which skips `right` when `left` succeeds), `IOr` always
runs both stages and handles all four combinations:

| left \ right | `Some` | `None` |
|---|---|---|
| `Some` | `Ior.Both(l, r)` | `Ior.Left(l)` |
| `None` | `Ior.Right(r)` | `Yield.None` |

Statuses from both sides are always merged with `combine`.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.cats.*
```

```scala mdoc
object IsPositive extends SAMStage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    if (in > 0) Yield.Some(in, Status.Success, this) else Yield.None(Status.Success, this)
}

object IsEven extends SAMStage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    if (in % 2 == 0) Yield.Some(in, Status.Success, this)
    else Yield.None(Status.Success, this)
}

val ior = IOr(IsPositive, IsEven)
ior(4)   // both succeed: Ior.Both
ior(3)   // left only:    Ior.Left
ior(-2)  // right only:   Ior.Right
ior(-3)  // neither:      Yield.None
```

## Projections

`IOr.Left` and `IOr.Right` are projections for `cats.data.Ior`. Each extracts one side when present,
yielding nothing otherwise. `Ior.Both` satisfies both projections simultaneously.

```scala mdoc
import _root_.cats.data.Ior
```

```scala mdoc
val leftProj  = IOr.Left[Int]
val rightProj = IOr.Right[Int]

leftProj(Ior.Left(1))
leftProj(Ior.Right(2))
leftProj(Ior.Both(1, 2))

rightProj(Ior.Left(1))
rightProj(Ior.Right(2))
rightProj(Ior.Both(1, 2))
```
