# And

`And` applies `left` and `right` to the same input and produces a `(LO, RO)` tuple when both succeed.
Evaluation is short-circuit on the left: if `left` yields [`Yield.None`](../../core/classes/Yield.md), `right` is
not applied at all.

| left \ right | `Some` | `None` |
|---|---|---|
| `Some` | `Some((l, r), combined)` | `None(combined)` |
| `None` | — right skipped — | `None(left)` |

Statuses from both sides are merged with `combine`.

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

val and = And(IsPositive, IsEven)
and(4)   // both succeed
and(3)   // IsPositive succeeds, IsEven fails
and(-2)  // IsPositive fails, IsEven not applied
```
