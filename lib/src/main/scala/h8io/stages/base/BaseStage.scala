package h8io.stages.base

import h8io.stages.{Evolution, Stage}

trait BaseStage[-I, +O, +E] extends Stage[I, O, E] with BaseEvolution[I, O, E] {
  def skip(): Evolution[I, O, E] = this
  def dispose(): Unit = {}
}

object BaseStage {
  type Endo[T, +E] = BaseStage[T, T, E]
}
