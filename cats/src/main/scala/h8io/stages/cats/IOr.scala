package h8io.stages.cats

import cats.data.Ior
import h8io.stages
import h8io.stages.base.{BaseBinaryOperator, LeftProjection, RightProjection}
import h8io.stages.{Evolution, Stage, Yield}

/** A binary operator that applies two stages independently to the same input and combines their outputs into a
  * `cats.data.Ior` (inclusive-or).
  *
  * All four combinations of left/right presence are handled:
  *
  * | Left result  | Right result | Output           |
  * |:-------------|:-------------|:-----------------|
  * | `Yield.Some` | `Yield.Some` | `Ior.Both(l, r)` |
  * | `Yield.Some` | `Yield.None` | `Ior.Left(l)`    |
  * | `Yield.None` | `Yield.Some` | `Ior.Right(r)`   |
  * | `Yield.None` | `Yield.None` | `Yield.None`     |
  *
  * Unlike `h8io.stages.operators.And` (which short-circuits on the left) and `h8io.stages.operators.Or` (which
  * short-circuits on the right), `IOr` always applies both stages, making it the inclusive variant.
  *
  * Statuses from both stages are merged with `++`. The evolution pairs corresponding branches symmetrically (like
  * `h8io.stages.operators.IAnd`).
  *
  * @param left
  *   the stage producing the left side of the `Ior`
  * @param right
  *   the stage producing the right side of the `Ior`
  * @tparam I
  *   the shared input type (contravariant)
  * @tparam LO
  *   the left output type (covariant)
  * @tparam RO
  *   the right output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class IOr[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BaseBinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, Ior[LO, RO], E] {
  override def apply(in: I): Yield[I, Ior[LO, RO], E] =
    (left(in), right(in)) match {
      case (Yield.Some(leftOut, leftStatus, leftEvolution), Yield.Some(rightOut, rightStatus, rightEvolution)) =>
        Yield.Some(Ior.Both(leftOut, rightOut), leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
      case (Yield.Some(leftOut, leftStatus, leftEvolution), Yield.None(rightStatus, rightEvolution)) =>
        Yield.Some(Ior.Left(leftOut), leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
      case (Yield.None(leftStatus, leftEvolution), Yield.Some(rightOut, rightStatus, rightEvolution)) =>
        Yield.Some(Ior.Right(rightOut), leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
      case (Yield.None(leftStatus, leftEvolution), Yield.None(rightStatus, rightEvolution)) =>
        Yield.None(leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
    }

  override def skip(): Evolution[I, Ior[LO, RO], E] = IOr.Evolution(left.skip(), right.skip())
}

/** Companion object for [[IOr]], containing the private evolution and projections. */
object IOr {
  private final case class Evolution[-I, +LO, +RO, +E](
      left: stages.Evolution[I, LO, E],
      right: stages.Evolution[I, RO, E])
      extends stages.Evolution[I, Ior[LO, RO], E] {
    override def onSuccess(): Stage[I, Ior[LO, RO], E] = IOr(left.onSuccess(), right.onSuccess())
    override def onComplete(): Stage[I, Ior[LO, RO], E] = IOr(left.onComplete(), right.onComplete())
    override def onError(): Stage[I, Ior[LO, RO], E] = IOr(left.onError(), right.onError())
  }

  /** Extracts the left value from an `cats.data.Ior`, yielding it when present in both `Ior.Left` and `Ior.Both`.
    *
    *   - `Ior.Left(l)` → `Yield.Some(l, ...)`
    *   - `Ior.Both(l, _)` → `Yield.Some(l, ...)`
    *   - `Ior.Right(_)` → `Yield.None(...)`
    *
    * Use `IOr.Left[T]` to get a typed `Projection[Ior[T, ?], T]`.
    */
  object Left extends LeftProjection[Ior] {
    override def apply(in: Ior[Any, ?]): Yield[Ior[Any, ?], Any, Nothing] =
      in.fold(out => some(out), _ => none, (out, _) => some(out))
  }

  /** Extracts the right value from an `cats.data.Ior`, yielding it when present in both `Ior.Right` and `Ior.Both`.
    *
    *   - `Ior.Right(r)` → `Yield.Some(r, ...)`
    *   - `Ior.Both(_, r)` → `Yield.Some(r, ...)`
    *   - `Ior.Left(_)` → `Yield.None(...)`
    *
    * Use `IOr.Right[T]` to get a typed `Projection[Ior[?, T], T]`.
    */
  object Right extends RightProjection[Ior] {
    override def apply(in: Ior[?, Any]): Yield[Ior[?, Any], Any, Nothing] =
      in.fold(_ => none, out => some(out), (_, out) => some(out))
  }
}
