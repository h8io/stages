package h8io.stages.std

import h8io.stages.base.{BaseStage, Fruitful}
import h8io.stages.{Signal, Yield}

object Break extends BaseStage.Endo[Any, Nothing] with Fruitful.Endo[Any, Nothing] {
  def apply[T]: Fruitful.Endo[T, Nothing] = asInstanceOf[Fruitful.Endo[T, Nothing]]

  override def apply(in: Any): Yield.Some[Any, Any, Nothing] = Yield.Some(in, Signal.Complete, this)
}
