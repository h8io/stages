package h8io.stages.operators

import h8io.stages.base.{BaseDecorator, Fruitful}
import h8io.stages.{Stage, Yield}

object KeepLastOutput {
  private[operators] final case class None[-I, +O, +E](alterand: Stage[I, O, E]) extends BaseDecorator[I, O, E] {
    override def apply(in: I): Yield[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, status, evolution) => Yield.Some(out, status, evolution.map(Some(out, _)))
        case Yield.None(status, evolution) => Yield.None(status, evolution.map(None(_)))
      }
  }

  private[operators] final case class Some[-I, +O, +E](out: O, alterand: Stage[I, O, E])
      extends BaseDecorator[I, O, E] with Fruitful[I, O, E] {
    override def apply(in: I): Yield.Some[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, status, evolution) => Yield.Some(out, status, evolution.map(Some(out, _)))
        case Yield.None(status, evolution) => Yield.Some(out, status, evolution.map(Some(out, _)))
      }
  }

  def apply[I, O, E](stage: Stage[I, O, E]): Stage[I, O, E] = None(stage)
}
