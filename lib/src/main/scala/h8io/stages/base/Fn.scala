package h8io.stages.base

import h8io.stages.{Status, Yield}

trait Fn[-I, +O] extends Fruitful[I, O, Nothing] with StageWithEvolution[I, O, Nothing] {
  def f(in: I): O

  override final def apply(in: I): Yield.Some[I, O, Nothing] = Yield.Some(f(in), Status.Success, this)
}

object Fn {
  type Endo[T] = Fn[T, T]
}
