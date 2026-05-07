package h8io.stages.base

import h8io.stages.Status

/** The result type returned by [[StaticStage.process]].
  *
  * `StaticYield` mirrors [[h8io.stages.Yield]] but omits the [[h8io.stages.Evolution]] field, since [[StaticStage]]
  * always uses `this` as the continuation. `apply` in [[StaticStage]] wraps a `StaticYield` into a full
  * [[h8io.stages.Yield]] by supplying the evolution automatically.
  *
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
sealed trait StaticYield[+O, +E] {
  def status: Status[E]
}

/** Companion object for [[StaticYield]]. */
object StaticYield {

  /** A [[StaticYield]] that carries an output value.
    *
    * @param out
    *   the output value produced by the stage
    * @param status
    *   the execution status
    */
  final case class Some[+O, +E](out: O, status: Status[E]) extends StaticYield[O, E]

  /** A [[StaticYield]] that carries no output value.
    *
    * @param status
    *   the execution status
    */
  final case class None[+E](status: Status[E]) extends StaticYield[Nothing, E]
}
