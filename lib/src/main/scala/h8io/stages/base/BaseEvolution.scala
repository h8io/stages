package h8io.stages.base

import h8io.stages.{Evolution, Stage}

trait BaseEvolution[-I, +O, +E] extends Evolution[I, O, E] {
  self: Stage[I, O, E] =>
  override def onSuccess(): Stage[I, O, E] = this
  override def onComplete(): Stage[I, O, E] = this
  override def onError(): Stage[I, O, E] = this
}

object BaseEvolution {
  type Endo[T, +E] = BaseEvolution[T, T, E]
}
