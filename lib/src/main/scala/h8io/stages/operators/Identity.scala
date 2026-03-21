package h8io.stages.operators

import h8io.stages.{Decoration, Stage}

object Identity extends Decoration[Any, Nothing, Nothing] {
  def apply[I, O, E]: Decoration[I, O, E] = asInstanceOf[Decoration[I, O, E]]

  def apply(stage: Stage[Any, Nothing, Nothing]): Stage[Any, Nothing, Nothing] = stage
}
