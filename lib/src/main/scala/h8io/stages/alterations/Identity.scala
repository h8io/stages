package h8io.stages.alterations

import h8io.stages.Stage
import h8io.stages.base.Decoration

object Identity extends Decoration[Any, Nothing, Nothing] {
  def apply[I, O, E]: Decoration[I, O, E] = asInstanceOf[Decoration[I, O, E]]

  def apply(stage: Stage[Any, Nothing, Nothing]): Stage[Any, Nothing, Nothing] = stage
}
