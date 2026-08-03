package h8io.stages.base

import h8io.stages.{Evolution, Stage, Status}

/** An `Evolution` that returns the same constant `Stage` regardless of the status.
  *
  * Used by `StageOps.toEvolution` to lift a stage into an evolution. Being deaf to the status is the point wherever the
  * status reaching this evolution is not the one the constant stage should be chosen by — that is what makes it the
  * evolution the `h8io.stages.cycles` operators return outwards.
  *
  * Disposal is not tied to the constant stage: it runs whatever `_dispose` holds, so the caller decides what this
  * evolution is the cleanup handle for.
  *
  * @param stage
  *   the stage returned by every branch
  * @param _dispose
  *   the thunk `dispose` delegates to; a no-op when built by the single-argument `ConstEvolution.apply`
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class ConstEvolution[-I, +O, +E](stage: Stage[I, O, E], _dispose: () => Unit) extends Evolution[I, O, E] {
  override def evolve(status: Status[?]): Stage[I, O, E] = stage
  override def dispose(): Unit = _dispose()
}

/** Companion object for [[ConstEvolution]]. */
object ConstEvolution {

  /** Creates a [[ConstEvolution]] with nothing to release: `dispose` is a no-op.
    *
    * @param stage
    *   the stage returned by every branch
    */
  def apply[I, O, E](stage: Stage[I, O, E]): ConstEvolution[I, O, E] = new ConstEvolution(stage, () => ())
}
