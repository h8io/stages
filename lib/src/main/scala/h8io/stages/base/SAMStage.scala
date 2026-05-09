package h8io.stages.base

import h8io.stages.{Evolution, Stage}

/** A `Stage` with a default implementation of `skip`.
  *
  * `skip` returns `this`, so every status branch of the evolution evaluates to `this`. A skipped `SAMStage` is
  * therefore indistinguishable from an active one that evolves back to itself. This is appropriate for stateless or
  * self-contained stages (e.g. [[h8io.stages.std.Complete]], [[h8io.stages.std.GlobalSoftDeadline]]) that carry no
  * external resources and always use the same instance as the next stage.
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
trait SAMStage[-I, +O, +E] extends Stage[I, O, E] with Stagnation[I, O, E] {
  override final def skip(): Evolution[I, O, E] = this
}

/** Companion object for [[SAMStage]]. */
object SAMStage {

  /** Type alias for an endomorphic [[SAMStage]] (same input and output type). */
  type Endo[T, +E] = SAMStage[T, T, E]
}
