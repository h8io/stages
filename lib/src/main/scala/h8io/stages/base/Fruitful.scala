package h8io.stages.base

import h8io.stages.{Stage, Yield}

trait Fruitful[-I, +O, +E] extends Stage[I, O, E] {
  override def apply(in: I): Yield.Some[I, O, E]
}

object Fruitful {
  type Endo[T, +E] = Fruitful[T, T, E]
}
