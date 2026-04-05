package h8io.stages.operators

import h8io.stages
import h8io.stages.base.BaseBinaryOperator
import h8io.stages.{Stage, Yield}

/** A binary operator that tries `left` first; if `left` produces no output, it falls back to `right`, wrapping the
  * result in `scala.util.Either`.
  *
  * Evaluation rules for a given input:
  *
  *   - If `left` yields `h8io.stages.Yield.Some`, the result is `Yield.Some(Left(leftOut), leftStatus, ...)`. `right`
  *     is **not** applied.
  *   - If `left` yields `h8io.stages.Yield.None`, `right` is applied:
  *     - `right` yields `Some` → `Yield.Some(Right(rightOut), mergedStatus, ...)`.
  *     - `right` yields `None` → `Yield.None(mergedStatus, ...)`.
  *
  * Statuses are merged with `++`.
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
      case Yield.Some(out, status, evolution) =>
        Yield.Some(Left(out), status, Or.Evolution(evolution, right.skip()))
      case Yield.None(leftStatus, leftEvolution) =>
        right(in) match {
          case Yield.Some(out, rightStatus, rightEvolution) =>
            Yield.Some(Right(out), leftStatus ++ rightStatus, Or.Evolution(leftEvolution, rightEvolution))
          case Yield.None(rightStatus, rightEvolution) =>
            Yield.None(leftStatus ++ rightStatus, Or.Evolution(leftEvolution, rightEvolution))
        }
    }

  override def skip(): stages.Evolution[I, Either[LO, RO], E] = Or.Evolution(left.skip(), right.skip())
}

/** Companion object for [[Or]]. */
object Or {
  private final case class Evolution[-I, +LO, +RO, +E](
      leftEvolution: stages.Evolution[I, LO, E],
      rightEvolution: stages.Evolution[I, RO, E])
      extends stages.Evolution[I, Either[LO, RO], E] {
    override def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onSuccess(), rightEvolution.onSuccess())
    override def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onComplete(), rightEvolution.onComplete())
    override def onError(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onError(), rightEvolution.onError())
  }
}
