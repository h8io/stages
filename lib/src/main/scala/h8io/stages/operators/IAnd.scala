package h8io.stages.operators

import h8io.stages
import h8io.stages.base.{BaseBinaryOperator, BinaryOperator}
import h8io.stages.{Evolution, Stage, Yield}

/** An ''independent'' binary operator that applies two stages to the same input and combines their outputs into a
  * tuple.
  *
  * Both `left` and `right` are always applied — unlike [[And]], which skips `right` if `left` produces no output.
  *
  *   - If both stages yield `h8io.stages.Yield.Some`, the result is `h8io.stages.Yield.Some``((leftOut, rightOut),
  *     mergedStatus, ...)`.
  *   - If either stage yields `h8io.stages.Yield.None`, the result is `h8io.stages.Yield.None``(mergedStatus, ...)`.
  *
  * Statuses from both stages are merged with `combine` (i.e., `leftStatus.combine(rightStatus)`). The evolution pairs
  * each branch's continuations symmetrically.
  *
  * @param left
  *   the first stage
  * @param right
  *   the second stage
  * @tparam I
  *   the shared input type (contravariant)
  * @tparam LO
  *   the left output type (covariant)
  * @tparam RO
  *   the right output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class IAnd[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, (LO, RO), E] {
  override def apply(in: I): Yield[I, (LO, RO), E] =
    (left(in), right(in)) match {
      case (Yield.Some(leftOut, leftStatus, leftEvolution), Yield.Some(rightOut, rightStatus, rightEvolution)) =>
        Yield.Some((leftOut, rightOut), leftStatus.combine(rightStatus), IAnd.Evolution(leftEvolution, rightEvolution))
      case (left, right) =>
        Yield.None(left.status.combine(right.status), IAnd.Evolution(left.evolution, right.evolution))
    }

  override def skip(): Evolution[I, (LO, RO), E] = IAnd.Evolution(left.skip(), right.skip())
}

/** Companion object for [[IAnd]]. */
object IAnd {
  private final case class Evolution[-I, LO, RO, +E](
      left: stages.Evolution[I, LO, E],
      right: stages.Evolution[I, RO, E])
      extends BaseBinaryOperator.Evolution[I, LO, RO, (LO, RO), E] {
    override protected def apply[_I <: I, _E >: E](leftStage: Stage[_I, LO, _E], rightStage: Stage[_I, RO, _E])
        : Stage[_I, (LO, RO), _E] = IAnd(leftStage, rightStage)
  }
}
