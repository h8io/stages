package h8io.stages.base

import h8io.stages.{Evolution, Stage}

/** An `Evolution` that returns the same constant `Stage` for all three status branches.
  *
  * Used by [[StageOps.toEvolution]] to lift a stateless stage into an evolution without a [[BaseEvolution]] mixin.
  * Equivalent to the old `BaseEvolution` mixin, but as a standalone value rather than an inherited trait.
  *
  * @param stage
  *   the stage returned by every branch
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class ConstEvolution[-I, +O, +E](stage: Stage[I, O, E]) extends Evolution[I, O, E] {
  override def onSuccess(): Stage[I, O, E] = stage
  override def onComplete(): Stage[I, O, E] = stage
  override def onError(): Stage[I, O, E] = stage
}
