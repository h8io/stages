package h8io.xi.stages.projections

import h8io.xi.stages.Yield

object Either {
  object Left extends LeftProjection[Either] {
    override def apply(in: Either[Any, ?]): Yield[Either[Any, ?], Any, Nothing] = in.fold(some, _ => none)
  }

  object Right extends RightProjection[Either] {
    override def apply(in: Either[?, Any]): Yield[Either[?, Any], Any, Nothing] = in.fold(_ => none, some)
  }
}
