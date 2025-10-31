package h8io.xi.stages.std

import h8io.xi.stages.{Signal, Yield}

trait Fn[-I, +O] extends Fruitful[I, O, Nothing] {
  def f(in: I): O

  final def apply(in: I): Yield.Some[I, O, Nothing] = Yield.Some(f(in), Signal.Success, this)
}

object Fn {
  type Endo[T] = Fn[T, T]
}
