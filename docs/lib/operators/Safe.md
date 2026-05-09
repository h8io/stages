# Safe

`Safe` catches non-fatal exceptions thrown by the inner stage and converts them into
[`Status.Complete`](../../core/classes/Status.md) error values, widening the error type from `E` to
`Either[Throwable, E]`.

- Exceptions caught during `apply` are reported as `Left(throwable)` in
  [`Yield.None`](../../core/classes/Yield.md)`(Status.error(Left(e)), ...)`.
- Errors already carried by the inner stage are wrapped as `Right(e)`.

Fatal exceptions (those not matched by `scala.util.control.NonFatal`) propagate normally. The evolution is
mapped so that every continuation stage remains wrapped in `Safe`.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.operators.*
```

```scala mdoc
object ParseIntUnsafe extends SAMStage[String, Int, Nothing] {
  override def apply(in: String): Yield[String, Int, Nothing] =
    Yield.Some(in.toInt, Status.Success, this)
}

val safe = Safe(ParseIntUnsafe)
safe("42")
safe("hello")
```
