package h8io.stages.base

import h8io.stages.{OnDone, Stage}

trait StageWithEvolution[-I, +O, +E] extends Stage[I, O, E] with OnDone[I, O, E] {
  self: Stage[I, O, E] =>
  override def onSuccess(): Stage[I, O, E] = this
  override def onComplete(): Stage[I, O, E] = this
  override def onError(): Stage[I, O, E] = this
}

object StageWithEvolution {
  type Endo[T, +E] = StageWithEvolution[T, T, E]
}
