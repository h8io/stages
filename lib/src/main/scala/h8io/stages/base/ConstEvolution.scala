package h8io.stages.base

import h8io.stages.{Evolution, Stage}

final case class ConstEvolution[-I, +O, +E](stage: Stage[I, O, E]) extends Evolution[I, O, E] {
  override def onSuccess(): Stage[I, O, E] = stage
  override def onComplete(): Stage[I, O, E] = stage
  override def onError(): Stage[I, O, E] = stage
}
