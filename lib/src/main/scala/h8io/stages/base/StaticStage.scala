package h8io.stages.base

import h8io.stages.{Evolution, Stage, Yield}

trait StaticStage[-I, +O, +E] extends Stage[I, O, E] with Stagnation[I, O, E] {
  override final def apply(in: I): Yield[I, O, E] =
    process(in) match {
      case StaticYield.Some(out, status) => Yield.Some(out, status, this)
      case StaticYield.None(status) => Yield.None(status, this)
    }

  protected def process(in: I): StaticYield[O, E]

  override final def skip(): Evolution[I, O, E] = this
}
