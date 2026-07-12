# Safe

`Safe` catches non-fatal exceptions thrown by the inner stage and converts them into
[`Status.Complete`](../../core/classes/Status.md) error values, widening the error type from `E` to
`Either[Throwable, E]`.

- Exceptions caught during `apply` are reported as `Left(throwable)` in
  [`Yield.None`](../../core/classes/Yield.md)`(Status.error(Left(e)), ...)`.
- Errors already carried by the inner stage are wrapped as `Right(e)`.

Fatal exceptions (those not matched by `scala.util.control.NonFatal`) propagate normally. The evolution is
mapped so that every continuation stage remains wrapped in `Safe`.

`Safe` is a crutch for the simplest cases — _stateless_ stages. Per the
[Lifecycle](../../core/classes/Stage.md#the-lifecycle-apply-and-skip) contract, a stage that throws from `apply`
has already released its own resources, so the recovered continuation simply reuses the inner stage — retrying it
on the next input — and carries a no-op dispose. A stage that owns resources should be made safe by itself instead
of being wrapped: extend `SafeStage` and supply a `recover` with the appropriate cleanup (the
[`ConstEvolution`](../base/ConstEvolution.md) overload taking a `dispose` function exists for exactly this).

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
