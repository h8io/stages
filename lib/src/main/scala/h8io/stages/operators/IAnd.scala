package h8io.stages.operators

import h8io.stages
import h8io.stages.base.BaseBinaryOperator
import h8io.stages.{Evolution, Stage, Yield}

/** An ''independent'' binary operator that applies two stages to the same input simultaneously and combines their
  * outputs into a tuple.
  *
  * Both `left` and `right` are always applied — unlike [[And]], which skips `right` if `left` produces no output.
  *
  *   - If both stages yield `h8io.stages.Yield.Some`, the result is `h8io.stages.Yield.Some``((leftOut, rightOut),
  *     mergedStatus, ...)`.
  *   - If either stage yields `h8io.stages.Yield.None`, the result is `h8io.stages.Yield.None``(mergedStatus, ...)`.
  *
  * Statuses from both stages are merged with `++` (i.e., `leftStatus ++ rightStatus`). The evolution pairs each
  * branch's continuations symmetrically.
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
    extends BaseBinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, (LO, RO), E] {
  override def apply(in: I): Yield[I, (LO, RO), E] =
    (left(in), right(in)) match {
      case (Yield.Some(leftOut, leftStatus, leftEvolution), Yield.Some(rightOut, rightStatus, rightEvolution)) =>
        Yield.Some((leftOut, rightOut), leftStatus ++ rightStatus, IAnd.Evolution(leftEvolution, rightEvolution))
      case (left, right) => Yield.None(left.status ++ right.status, IAnd.Evolution(left.evolution, right.evolution))
    }

  override def skip(): Evolution[I, (LO, RO), E] = IAnd.Evolution(left.skip(), right.skip())
}

/** Companion object for [[IAnd]]. */
object IAnd {
  private final case class Evolution[-I, +LO, +RO, +E](
      left: stages.Evolution[I, LO, E],
      right: stages.Evolution[I, RO, E])
      extends stages.Evolution[I, (LO, RO), E] {
    override def onSuccess(): Stage[I, (LO, RO), E] = IAnd(left.onSuccess(), right.onSuccess())
    override def onComplete(): Stage[I, (LO, RO), E] = IAnd(left.onComplete(), right.onComplete())
    override def onError(): Stage[I, (LO, RO), E] = IAnd(left.onError(), right.onError())
  }
}
