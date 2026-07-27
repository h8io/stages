package h8io.stages.operators

import h8io.stages.base.Alterator
import h8io.stages.{Evolution, Stage, Yield}

/** A decorator that wraps a stage's optional output into an `Option`, making the result always present.
  *
  *   - If the inner stage yields `h8io.stages.Yield.Some``(v, ...)`, `Lift` produces `h8io.stages.Yield.Some``(Some(v),
  *     ...)`.
  *   - If the inner stage yields `h8io.stages.Yield.None``(...)`, `Lift` produces `h8io.stages.Yield.Some``(None,
  *     ...)`.
  *
  * Because `Lift` always emits a value, it extends `h8io.stages.Stage.Fruitful`. The evolution of the result is mapped
  * so that every continuation stage is also wrapped in `Lift`, preserving the lifting semantics across the entire
  * pipeline — which is what lets the guarantee be stated for the whole lineage rather than for a single run.
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
    extends Alterator[Stage[I, O, E], I, Option[O], E] with Stage.Fruitful[I, Option[O], E] {
  override def apply(in: I): Yield.Some.Fruitful[I, Option[O], E] =
    alterand(in) match {
      case Yield.Some(out, status, evolution) =>
        Yield.Some.Fruitful(Some(out), status, evolution.mapToFruitful(Lift(_)))
      case Yield.None(status, evolution) => Yield.Some.Fruitful(None, status, evolution.mapToFruitful(Lift(_)))
    }

  override def skip(): Evolution.Fruitful[I, Option[O], E] = alterand.skip().mapToFruitful(Lift(_))
}
