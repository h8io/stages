package h8io.stages.base

import h8io.stages.{Evolution, Stage, Status}

/** An [[h8io.stages.Evolution]] that always returns itself as the next stage.
  *
  * Mixing in `Stagnation` fixes `evolve` to unconditionally return `this`, regardless of the [[h8io.stages.Status]]
  * passed in, and makes `dispose` a no-op. The stage is therefore stateless with respect to evolution: every pipeline
  * run produces the same continuation.
  *
  * Appropriate for stages that hold no external resources and whose behavior does not depend on the outcome of
  * previous runs. See [[StaticStage]] for a concrete template-method mixin built on top of `Stagnation`.
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait Stagnation[-I, +O, +E] extends Evolution[I, O, E] {
  self: Stage[I, O, E] =>
  override final def evolve(status: Status[?]): Stage[I, O, E] = this
  override def dispose(): Unit = ()
}
