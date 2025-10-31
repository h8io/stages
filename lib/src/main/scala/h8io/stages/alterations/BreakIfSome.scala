package h8io.stages.alterations

import h8io.stages.std.Break
import h8io.stages.{Decoration, Stage}

object BreakIfSome extends Decoration[Any, Any, Nothing] {
  def apply[I, O, E]: Decoration[I, O, E] = asInstanceOf[Decoration[I, O, E]]

  override def apply(stage: Stage[Any, Any, Nothing]): Stage[Any, Any, Nothing] = stage ~> Break
}
