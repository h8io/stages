package h8io.stages.alterations

import h8io.stages.base.BaseDecorator
import h8io.stages.{Signal, Stage, Yield}

final case class BreakIfNone[I, O, E](alterand: Stage[I, O, E]) extends BaseDecorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] =
    alterand(in) match {
      case Yield.None(Signal.Success, onDone) => Yield.None(Signal.Complete, onDone.map(BreakIfNone(_)))
      case other => other.mapOnDone(_.map(BreakIfNone(_)))
    }

  override protected def wrapAlterand(stage: Stage[I, O, E]): Stage[I, O, E] = BreakIfNone(stage)
}
