# IAnd

`IAnd` (Independent And) applies both `left` and `right` to the same input unconditionally and produces a
`(LO, RO)` tuple when both succeed. Unlike [`And`](And.md), neither side is short-circuited: both stages
always run.

| left \ right | `Some` | `None` |
|---|---|---|
| `Some` | `Some((l, r), combined)` | `None(combined)` |
| `None` | `None(combined)` | `None(combined)` |

Statuses from both sides are always merged with `combine`.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.operators.*
```

```scala mdoc
object IsPositive extends SAMStage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    if (in > 0) Yield.Some(in, Status.Success, this) else Yield.None(Status.Success, this)
}

object IsEven extends SAMStage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    if (in % 2 == 0) Yield.Some(in, Status.Success, this) else Yield.None(Status.Success, this)
}

val iand = IAnd(IsPositive, IsEven)
iand(4)   // both succeed
iand(3)   // IsPositive succeeds, IsEven fails: both still run
iand(-2)  // IsPositive fails, IsEven succeeds: both still run
```
