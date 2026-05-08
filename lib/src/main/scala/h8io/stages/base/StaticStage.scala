package h8io.stages.base

import h8io.stages.Yield

/** A `h8io.stages.Stage` that is its own `Evolution`.
  *
  * `StaticStage` seals `apply` and `skip`, delegating the actual processing logic to [[process]]. `apply` wraps the
  * [[StaticYield]] returned by `process` into a full `h8io.stages.Yield` with `this` as the evolution; `skip` returns
  * `this` directly.
  *
  * Mixing in `StaticStage` is appropriate when the stage does not change between runs and holds no external resources.
  * See [[Fn]] for the further-constrained variant that always produces an output with `h8io.stages.Status.Success`.
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait StaticStage[-I, +O, +E] extends SAMStage[I, O, E] {
  override final def apply(in: I): Yield[I, O, E] =
    process(in) match {
      case StaticYield.Some(out, status) => Yield.Some(out, status, this)
      case StaticYield.None(status) => Yield.None(status, this)
    }

  /** The processing logic for this stage.
    *
    * @param in
    *   the input value
    * @return
    *   a [[h8io.stages.base.StaticYield]] describing whether an output was produced and the resulting
    *   `h8io.stages.Status`
    */
  protected def process(in: I): StaticYield[O, E]
}

/** Companion object for [[StaticStage]]. */
object StaticStage {

  /** Type alias for an endomorphic [[StaticStage]] (same input and output type). */
  type Endo[T, +E] = StaticStage[T, T, E]
}
