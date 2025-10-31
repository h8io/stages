package h8io.stages.projections

import h8io.stages.Yield

object Either {
  object Left extends LeftProjection[Either] {
    override def apply(in: Either[Any, ?]): Yield[Either[Any, ?], Any, Nothing] = in.fold(some, _ => none)
  }

  object Right extends RightProjection[Either] {
    override def apply(in: Either[?, Any]): Yield[Either[?, Any], Any, Nothing] = in.fold(_ => none, some)
  }
}
