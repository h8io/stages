package h8io.stages.operators

import h8io.stages.base.{Alterator, SafeStage, StageOps}
import h8io.stages.{Evolution, Stage, Status, Yield}

/** A decorator that catches non-fatal exceptions thrown by the inner stage and converts them into
  * `h8io.stages.Status.Complete` error values.
  *
  * The error type is widened from `E` to `Either[Throwable, E]`:
  *   - Errors already carried by the inner stage are wrapped as `Right(e)`.
  *   - Exceptions caught during `apply` are reported as `Left(throwable)` in a
  *     `Yield.None(Status.error(Left(e)), this)`.
  *
  * Only non-fatal exceptions (matched by `scala.util.control.NonFatal`) are caught; fatal errors such as
  * `OutOfMemoryError` propagate normally.
  *
  * The evolution is mapped so that every continuation stage remains wrapped in `Safe`, maintaining exception safety
  * across the entire pipeline.
  *
  * `Safe` is a crutch for the simplest cases — ''stateless'' stages. Per the ''Lifecycle'' contract in
  * `h8io.stages.Stage`, a stage that throws from `apply` has already released its own resources, so the recovered
  * continuation simply reuses the inner stage — retrying it on the next input — and carries a no-op dispose. A stage
  * that owns resources should be made safe by itself instead of being wrapped: extend [[h8io.stages.base.SafeStage]]
  * and supply a `recover` with the appropriate cleanup (the `h8io.stages.base.ConstEvolution` overload taking a
  * `dispose` function exists for exactly this).
  *
  * @param alterand
  *   the inner stage whose exceptions should be caught
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the original error type of the inner stage (covariant)
  */
final case class Safe[-I, +O, +E](alterand: Stage[I, O, E])
    extends Alterator[Stage[I, O, E], I, O, Either[Throwable, E]] with SafeStage[I, O, Either[Throwable, E]] {
  override def body(in: I): Yield[I, O, Either[Throwable, E]] =
    alterand(in).map(identity, _.map(Right(_)), _.map(Safe(_)))

  override def recover(in: I, e: Throwable): Yield[I, O, Either[Throwable, E]] =
    Yield.None(Status.error(Left(e)), this.toEvolution)

  override def skip(): Evolution[I, O, Either[Throwable, E]] = alterand.skip().map(Safe(_))
}
