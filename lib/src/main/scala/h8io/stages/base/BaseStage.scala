package h8io.stages.base

import h8io.stages.{OnDone, Stage}

trait BaseStage[-I, +O, +E] extends Stage[I, O, E] with OnDoneForStage[I, O, E] {
  override def skip: OnDone[I, O, E] = this
  override def dispose(): Unit = ()
}

object BaseStage {
  type Endo[T, +E] = BaseStage[T, T, E]
}
