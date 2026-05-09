package h8io.stages.operators

import h8io.stages.base.{Alterator, Fruitful}
import h8io.stages.{Evolution, Stage, Yield}

/** A decorator that wraps a stage's optional output into an `Option`, making the result always present.
  *
  *   - If the inner stage yields `h8io.stages.Yield.Some``(v, ...)`, `Lift` produces `h8io.stages.Yield.Some``(Some(v),
  *     ...)`.
  *   - If the inner stage yields `h8io.stages.Yield.None``(...)`, `Lift` produces `h8io.stages.Yield.Some``(None,
  *     ...)`.
  *
  * Because `Lift` always emits a value, it extends [[h8io.stages.base.Fruitful]]. The evolution of the result is mapped
  * so that every continuation stage is also wrapped in `Lift`, preserving the lifting semantics across the entire
  * pipeline.
  *
  * `Lift` is the inverse of [[h8io.stages.projections.Unlift]].
  *
  * @param alterand
  *   the inner stage whose output is optionally present
  * @tparam I
  *   the input type
  * @tparam O
  *   the inner output type; the outer output type becomes `Option[O]`
  * @tparam E
  *   the error type
  */
final case class Lift[I, O, E](alterand: Stage[I, O, E])
    extends Alterator[Stage[I, O, E], I, Option[O], E] with Fruitful[I, Option[O], E] {
  override def apply(in: I): Yield.Some[I, Option[O], E] =
    alterand(in) match {
      case Yield.Some(out, status, evolution) => Yield.Some(Some(out), status, evolution.map(Lift(_)))
      case Yield.None(status, evolution) => Yield.Some(None, status, evolution.map(Lift(_)))
    }

  override def skip(): Evolution[I, Option[O], E] = alterand.skip().map(Lift(_))
}
