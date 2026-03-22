package h8io.stages.operators

import h8io.stages.Stage
import h8io.stages.base.Decoration
import h8io.stages.std.Break

object BreakIfSome extends Decoration[Any, Any, Nothing] {
  def apply[I, O, E]: Decoration[I, O, E] = asInstanceOf[Decoration[I, O, E]]

  override def apply(stage: Stage[Any, Any, Nothing]): Stage[Any, Any, Nothing] = stage ~> Break
}
