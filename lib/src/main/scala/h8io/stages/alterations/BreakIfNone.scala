package h8io.stages.alterations

import h8io.stages.base.BaseDecorator
import h8io.stages.{Stage, Status, Yield}

final case class BreakIfNone[I, O, E](alterand: Stage[I, O, E]) extends BaseDecorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] =
    alterand(in) match {
      case Yield.None(Status.Success, evolution) => Yield.None(Status.Complete, evolution.map(BreakIfNone(_)))
      case other => other.map(identity, identity, _.map(BreakIfNone(_)))
    }
}
