package h8io.stages.std

import h8io.stages.base.BaseEvolution
import h8io.stages.{Stage, Status, Yield}

object Unlift extends Stage[Option[Any], Any, Nothing] with BaseEvolution[Option[Any], Any, Nothing] {
  override def apply(in: Option[Any]): Yield[Option[Any], Any, Nothing] =
    in match {
      case Some(out) => Yield.Some(out, Status.Success, this)
      case None => Yield.None(Status.Success, this)
    }

  def apply[T]: Stage[Option[T], T, Nothing] = asInstanceOf[Stage[Option[T], T, Nothing]]
}
