package h8io.xi.stages.alterations

import h8io.xi.stages.std.Fruitful
import h8io.xi.stages.{Decorator, Stage, Yield}

object KeepLastOutput {
  private[alterations] final case class None[-I, +O, +E](alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
    def apply(in: I): Yield[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, signal, onDone) => Yield.Some(out, signal, onDone.map(Some(out, _)))
        case Yield.None(signal, onDone) => Yield.None(signal, onDone.map(None(_)))
      }
  }

  private[alterations] final case class Some[-I, +O, +E](out: O, alterand: Stage[I, O, E])
      extends Decorator[I, O, E] with Fruitful[I, O, E] {
    def apply(in: I): Yield.Some[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, signal, onDone) => Yield.Some(out, signal, onDone.map(Some(out, _)))
        case Yield.None(signal, onDone) => Yield.Some(out, signal, onDone.map(Some(out, _)))
      }
  }

  def apply[I, O, E](stage: Stage[I, O, E]): Stage[I, O, E] = None(stage)
}
