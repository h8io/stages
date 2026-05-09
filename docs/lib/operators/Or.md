# Or

`Or` tries `left` first; if `left` produces no output, it falls back to `right`. The result is wrapped in
`scala.util.Either` to distinguish which branch supplied the value. `right` is never applied when `left` succeeds.

| left | right | result |
|---|---|---|
| `Some(l)` | — not applied — | `Some(Left(l), leftStatus)` |
| `None` | `Some(r)` | `Some(Right(r), combined)` |
| `None` | `None` | `None(combined)` |

Statuses are merged with `combine` only when both sides are evaluated.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.operators.*
```

```scala mdoc
object ParseInt extends SAMStage[String, Int, Nothing] {
  override def apply(in: String): Yield[String, Int, Nothing] =
    in.toIntOption match {
      case Some(n) => Yield.Some(n, Status.Success, this)
      case None    => Yield.None(Status.Success, this)
    }
}

object ParseLength extends SAMStage[String, Int, Nothing] {
  override def apply(in: String): Yield[String, Int, Nothing] =
    Yield.Some(in.length, Status.Success, this)
}

val or = Or(ParseInt, ParseLength)
or("42")     // left succeeds: Left(42)
or("hello")  // left fails, right succeeds: Right(5)
```
