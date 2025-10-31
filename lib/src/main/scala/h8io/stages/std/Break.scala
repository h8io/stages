package h8io.stages.std

import h8io.stages.{Signal, Yield}

object Break extends Fruitful.Endo[Any, Nothing] {
  def apply[T]: Fruitful.Endo[T, Nothing] = asInstanceOf[Fruitful.Endo[T, Nothing]]

  def apply(in: Any): Yield.Some[Any, Any, Nothing] = Yield.Some(in, Signal.Complete, this)
}
