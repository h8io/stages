package h8io.stages.base

import h8io.stages.{OnDone, Stage}

trait OnDoneForStage[-I, +O, +E] extends OnDone[I, O, E] {
  self: Stage[I, O, E] =>

  override def onSuccess(): Stage[I, O, E] = this
  override def onComplete(): Stage[I, O, E] = this
  override def onError(): Stage[I, O, E] = this
}

object OnDoneForStage {
  type Endo[T, +E] = OnDoneForStage[T, T, E]
}
