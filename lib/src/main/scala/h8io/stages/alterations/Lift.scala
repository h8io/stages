package h8io.stages.alterations

import h8io.stages.base.{BaseAlterator, Fruitful}
import h8io.stages.{Stage, Yield}

final case class Lift[I, O, E](alterand: Stage[I, O, E])
    extends BaseAlterator[Stage[I, O, E], I, scala.Option[O], E] with Fruitful[I, scala.Option[O], E] {
  override def apply(in: I): Yield.Some[I, scala.Option[O], E] =
    alterand(in) match {
      case Yield.Some(out, status, onDone) => Yield.Some(Some(out), status, onDone.map(Lift(_)))
      case Yield.None(status, onDone) => Yield.Some(None, status, onDone.map(Lift(_)))
    }
}
