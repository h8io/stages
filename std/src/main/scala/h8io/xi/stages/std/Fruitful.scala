package h8io.xi.stages.std

import h8io.xi.stages.{Stage, Yield}

trait Fruitful[-I, +O, +E] extends Stage[I, O, E] {
  def apply(in: I): Yield.Some[I, O, E]
}

object Fruitful {
  type Endo[T, +E] = Fruitful[T, T, E]
}
