package h8io.stages.alterations

import h8io.stages.base.Decorator
import h8io.stages.{Signal, Stage, Yield}

final case class BreakIfNone[I, O, E](alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] =
    alterand(in) match {
      case Yield.None(Signal.Success, onDone) => Yield.None(Signal.Complete, onDone.map(BreakIfNone(_)))
      case other => other.mapOnDone(_.map(BreakIfNone(_)))
    }
}
