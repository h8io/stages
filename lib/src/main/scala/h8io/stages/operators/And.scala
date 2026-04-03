package h8io.stages.operators

import h8io.stages.base.BaseBinaryOperator
import h8io.stages.{Evolution, Stage, Yield}

/** A binary operator that applies `left` and `right` to the same input sequentially, producing a tuple of both outputs
  * when both succeed.
  *
  * The evaluation is short-circuit on the left side:
  *
  *   - If `left` yields `h8io.stages.Yield.Some`, `right` is applied to the same input.
  *     - If `right` also yields `Some`, the result is `Yield.Some((leftOut, rightOut), ...)`.
  *     - If `right` yields `None`, the result is `Yield.None(mergedStatus, ...)`.
  *   - If `left` yields `h8io.stages.Yield.None`, `right` is **not** applied and the result is
  *     `Yield.None(leftStatus, ...)`. The right stage is preserved in the evolution so that it will be applied once the
  *     left side eventually produces output.
  *
  * Statuses from both sides are merged with `++`.
  *
  * Two private evolution implementations track whether the right stage's evolution is already known (`BothEvolution`)
  * or still pending (`LeftEvolution`).
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
            Yield.Some((leftOut, rightOut), leftStatus ++ rightStatus, And.BothEvolution(leftEvolution, rightEvolution))
          case Yield.None(rightStatus, rightEvolution) =>
            Yield.None(leftStatus ++ rightStatus, And.BothEvolution(leftEvolution, rightEvolution))
        }
      case Yield.None(status, evolution) => Yield.None(status, And.LeftEvolution(evolution, right))
    }
}

/** Companion object for [[And]]. */
object And {
  private final case class LeftEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E], right: Stage[I, RO, E])
      extends Evolution[I, (LO, RO), E] {
    override def onSuccess(): Stage[I, (LO, RO), E] = And(leftEvolution.onSuccess(), right)
    override def onComplete(): Stage[I, (LO, RO), E] = And(leftEvolution.onComplete(), right)
    override def onError(): Stage[I, (LO, RO), E] = And(leftEvolution.onError(), right)
  }

  private final case class BothEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E],
      rightEvolution: Evolution[I, RO, E])
      extends Evolution[I, (LO, RO), E] {
    override def onSuccess(): Stage[I, (LO, RO), E] = And(leftEvolution.onSuccess(), rightEvolution.onSuccess())
    override def onComplete(): Stage[I, (LO, RO), E] = And(leftEvolution.onComplete(), rightEvolution.onComplete())
    override def onError(): Stage[I, (LO, RO), E] = And(leftEvolution.onError(), rightEvolution.onError())
  }
}
