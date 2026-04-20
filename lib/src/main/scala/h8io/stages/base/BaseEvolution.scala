package h8io.stages.base

import h8io.stages.{Evolution, Stage}

/** A mixin `h8io.stages.Evolution` whose every branch returns `this` — the stage that mixes it in.
  *
  * This is the simplest possible evolution strategy: regardless of whether the pipeline signals
  * `h8io.stages.Status.Success`, `h8io.stages.Status.Complete`, or `h8io.stages.Status.Error`, the same stage is used
  * for the next invocation. It is appropriate for stateless, pure stages (e.g. [[Fn]], [[Projection]]) that do not need
  * to change behavior between invocations.
  *
  * The self-type constraint `self: Stage[I, O, E]` ensures that `BaseEvolution` can only be mixed into a compatible
  * `h8io.stages.Stage`.
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait BaseEvolution[-I, +O, +E] extends Evolution[I, O, E] {
  self: Stage[I, O, E] =>
  override def onSuccess(): Stage[I, O, E] = this
  override def onComplete(): Stage[I, O, E] = this
  override def onError(): Stage[I, O, E] = this

  override def dispose(): Unit = ()
}

/** Companion object for [[BaseEvolution]]. */
object BaseEvolution {

  /** Type alias for an endomorphic [[BaseEvolution]] (same input and output type). */
  type Endo[T, +E] = BaseEvolution[T, T, E]
}
