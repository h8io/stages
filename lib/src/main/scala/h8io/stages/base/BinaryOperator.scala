package h8io.stages.base

import h8io.stages
import h8io.stages.Stage

/** A `h8io.stages.Stage` that composes two sub-stages operating on the same input type.
  *
  * `BinaryOperator` provides exception-safe disposal of both sub-stages: `right.dispose()` is called first, then
  * `left.dispose()`. If `right.dispose()` throws, `left.dispose()` is still attempted and any exception it throws is
  * attached as a suppressed exception to the primary one.
  *
  * Concrete subclasses (e.g. [[h8io.stages.operators.And]], [[h8io.stages.operators.Or]],
  * [[h8io.stages.operators.IAnd]]) implement `apply` to define how the two sub-stages are combined.
  *
  * @tparam LS
  *   the type of the left sub-stage (covariant, must accept input `I`)
  * @tparam RS
  *   the type of the right sub-stage (covariant, must accept input `I`)
  * @tparam I
  *   the shared input type (contravariant)
  * @tparam O
  *   the combined output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait BinaryOperator[+LS <: Stage[I, ?, ?], +RS <: Stage[I, ?, ?], -I, +O, +E] extends Stage[I, O, E] {

  /** The left sub-stage. */
  def left: LS

  /** The right sub-stage. */
  def right: RS
}

object BinaryOperator {
  trait Evolution[+LE <: stages.Evolution[I, ?, ?], +RE <: stages.Evolution[I, ?, ?], -I, +O, +E]
      extends stages.Evolution[I, O, E] {
    def left: LE
    def right: RE

    override def dispose(): Unit = right.dispose(); left.dispose()
  }
}
