package h8io.stages.std

import h8io.stages.base.{Fruitful, StageWithEvolution}
import h8io.stages.{Status, Yield}

object Break extends Fruitful.Endo[Any, Nothing] with StageWithEvolution.Endo[Any, Nothing] {
  def apply[T]: Fruitful.Endo[T, Nothing] = asInstanceOf[Fruitful.Endo[T, Nothing]]

  override def apply(in: Any): Yield.Some[Any, Any, Nothing] = Yield.Some(in, Status.Complete, this)
}
