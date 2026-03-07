package h8io.stages.alterations

import h8io.stages.base.{BaseDecorator, Fruitful}
import h8io.stages.{Stage, Yield}

object KeepLastOutput {
  private[alterations] final case class None[I, O, E](alterand: Stage[I, O, E]) extends BaseDecorator[I, O, E] {
    override def apply(in: I): Yield[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, signal, onDone) => Yield.Some(out, signal, onDone.map(Some(out, _)))
        case Yield.None(signal, onDone) => Yield.None(signal, onDone.map(None(_)))
      }

    override protected def withAlterand(stage: Stage[I, O, E]): Stage[I, O, E] = None(stage)
  }

  private[alterations] final case class Some[I, O, E](out: O, alterand: Stage[I, O, E])
      extends BaseDecorator[I, O, E] with Fruitful[I, O, E] {
    override def apply(in: I): Yield.Some[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, signal, onDone) => Yield.Some(out, signal, onDone.map(Some(out, _)))
        case Yield.None(signal, onDone) => Yield.Some(out, signal, onDone.map(Some(out, _)))
      }

    override protected def withAlterand(stage: Stage[I, O, E]): Stage[I, O, E] = Some(out, stage)
  }

  def apply[I, O, E](stage: Stage[I, O, E]): Stage[I, O, E] = None(stage)
}
