package h8io.stages.operators

import h8io.stages.base.{BaseUnaryOperator, Fruitful}
import h8io.stages.{Stage, Yield}

final case class Lift[I, O, E](alterand: Stage[I, O, E])
    extends BaseUnaryOperator[Stage[I, O, E], I, Option[O], E] with Fruitful[I, Option[O], E] {
  override def apply(in: I): Yield.Some[I, Option[O], E] =
    alterand(in) match {
      case Yield.Some(out, status, evolution) => Yield.Some(Some(out), status, evolution.map(Lift(_)))
      case Yield.None(status, evolution) => Yield.Some(None, status, evolution.map(Lift(_)))
    }
}
