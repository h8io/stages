package h8io.stages.base

import h8io.stages.{Evolution, Stage}

/** A `Stage` with default implementations of `skip` and `dispose`.
  *
  * [[BaseStage]] mixes in [[BaseEvolution]], so every branch of the evolution returns `this`. The `skip` implementation
  * follows the same convention: it returns `this`, meaning a skipped `BaseStage` is indistinguishable from an active
  * one that would evolve back to itself. This is appropriate for stateless or self-contained stages (e.g. [[Fn]],
  * [[h8io.stages.std.GlobalSoftDeadline]]) that carry no external resources and always use the same instance as the
  * next stage.
  *
  * `dispose` is a no-op by default. Override it when the stage holds external resources that must be released (see
  * [[h8io.stages.std.DeadEnd]] for an example).
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait BaseStage[-I, +O, +E] extends Stage[I, O, E] {
  override def skip(): Evolution[I, O, E] = ConstEvolution(this)
}

/** Companion object for [[BaseStage]]. */
object BaseStage {

  /** Type alias for an endomorphic [[BaseStage]] (same input and output type). */
  type Endo[T, +E] = BaseStage[T, T, E]
}
