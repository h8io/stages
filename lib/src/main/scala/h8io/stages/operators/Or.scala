package h8io.stages.operators

import h8io.stages.base.BaseBinaryOperator
import h8io.stages.{Evolution, Stage, Yield}

/** A binary operator that tries `left` first; if `left` produces no output, it falls back to `right`, wrapping the
  * result in `scala.util.Either`.
  *
  * Evaluation rules for a given input:
  *
  *   - If `left` yields `h8io.stages.Yield.Some`, the result is
  *     `Yield.Some(Left(leftOut), leftStatus, LeftEvolution(...))`. `right` is **not** applied.
  *   - If `left` yields `h8io.stages.Yield.None`, `right` is applied:
  *     - `right` yields `Some` → `Yield.Some(Right(rightOut), mergedStatus, BothEvolution(...))`.
  *     - `right` yields `None` → `Yield.None(mergedStatus, BothEvolution(...))`.
  *
  * Statuses are merged with `++`.
  *
  * Two private evolution implementations track whether the left side succeeded on its own (`LeftEvolution`, right not
  * yet consumed) or both sides were evaluated (`BothEvolution`).
  *
  * @param left
  *   the preferred stage; tried first
  * @param right
  *   the fallback stage; tried only when `left` yields no output
  * @tparam I
  *   the shared input type (contravariant)
  * @tparam LO
  *   the left output type (covariant)
  * @tparam RO
  *   the right output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class Or[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BaseBinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, Either[LO, RO], E] {
  override def apply(in: I): Yield[I, Either[LO, RO], E] =
    left(in) match {
      case Yield.Some(out, status, evolution) => Yield.Some(Left(out), status, Or.LeftEvolution(evolution, right))
      case Yield.None(leftStatus, leftEvolution) =>
        right(in) match {
          case Yield.Some(out, rightStatus, rightEvolution) =>
            Yield.Some(Right(out), leftStatus ++ rightStatus, Or.BothEvolution(leftEvolution, rightEvolution))
          case Yield.None(rightStatus, rightEvolution) =>
            Yield.None(leftStatus ++ rightStatus, Or.BothEvolution(leftEvolution, rightEvolution))
        }
    }
}

/** Companion object for [[Or]]. */
object Or {
  private final case class LeftEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E], right: Stage[I, RO, E])
      extends Evolution[I, Either[LO, RO], E] {
    override def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onSuccess(), right)
    override def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onComplete(), right)
    override def onError(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onError(), right)
  }

  private final case class BothEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E],
      rightEvolution: Evolution[I, RO, E])
      extends Evolution[I, Either[LO, RO], E] {
    override def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onSuccess(), rightEvolution.onSuccess())
    override def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onComplete(), rightEvolution.onComplete())
    override def onError(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onError(), rightEvolution.onError())
  }
}
