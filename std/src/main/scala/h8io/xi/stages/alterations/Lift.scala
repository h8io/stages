package h8io.xi.stages.alterations

import h8io.xi.stages.std.Fruitful
import h8io.xi.stages.{Alterator, Stage, Yield}

final case class Lift[I, O, E](alterand: Stage[I, O, E])
    extends Alterator[Stage[I, O, E], I, scala.Option[O], E] with Fruitful[I, scala.Option[O], E] {
  def apply(in: I): Yield.Some[I, scala.Option[O], E] =
    alterand(in) match {
      case Yield.Some(out, signal, onDone) => Yield.Some(Some(out), signal, onDone.map(Lift(_)))
      case Yield.None(signal, onDone) => Yield.Some(None, signal, onDone.map(Lift(_)))
    }
}
