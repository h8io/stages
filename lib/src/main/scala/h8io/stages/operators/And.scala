package h8io.stages.operators

import h8io.stages
import h8io.stages.base.BaseBinaryOperator
import h8io.stages.{Stage, Yield}

/** A binary operator that applies `left` and `right` to the same input sequentially, producing a tuple of both outputs
  * when both succeed.
  *
  * The evaluation is short-circuit on the left side:
  *
  *   - If `left` yields `h8io.stages.Yield.Some`, `right` is applied to the same input.
  *     - If `right` also yields `Some`, the result is `Yield.Some((leftOut, rightOut), ...)`.
  *     - If `right` yields `None`, the result is `Yield.None(mergedStatus, ...)`.
  *   - If `left` yields `h8io.stages.Yield.None`, `right` is **not** applied and the result is
  *     `Yield.None(leftStatus, ...)`.
  *
  * Statuses from both sides are merged with `++`.
  *
  * @param left
  *   the first stage to apply
  * @param right
  *   the second stage to apply when `left` succeeds
  * @tparam I
  *   the shared input type (contravariant)
  * @tparam LO
  *   the left output type (covariant)
  * @tparam RO
  *   the right output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class And[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BaseBinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, (LO, RO), E] {
  override def apply(in: I): Yield[I, (LO, RO), E] =
    left(in) match {
      case Yield.Some(leftOut, leftStatus, leftEvolution) => right(in) match {
          case Yield.Some(rightOut, rightStatus, rightEvolution) =>
            Yield.Some((leftOut, rightOut), leftStatus ++ rightStatus, And.Evolution(leftEvolution, rightEvolution))
          case Yield.None(rightStatus, rightEvolution) =>
            Yield.None(leftStatus ++ rightStatus, And.Evolution(leftEvolution, rightEvolution))
        }
      case Yield.None(status, evolution) => Yield.None(status, And.Evolution(evolution, right.skip()))
    }

  override def skip(): stages.Evolution[I, (LO, RO), E] = And.Evolution(left.skip(), right.skip())
}

/** Companion object for [[And]]. */
object And {
  private final case class Evolution[-I, +LO, +RO, +E](
      leftEvolution: stages.Evolution[I, LO, E],
      rightEvolution: stages.Evolution[I, RO, E])
      extends stages.Evolution[I, (LO, RO), E] {
    override def onSuccess(): Stage[I, (LO, RO), E] = And(leftEvolution.onSuccess(), rightEvolution.onSuccess())
    override def onComplete(): Stage[I, (LO, RO), E] = And(leftEvolution.onComplete(), rightEvolution.onComplete())
    override def onError(): Stage[I, (LO, RO), E] = And(leftEvolution.onError(), rightEvolution.onError())
  }
}
