package h8io.xi.stages.projections

import h8io.xi.stages.Yield

object Validated {
  type Validated[+L, +R] = cats.data.Validated[L, R]

  object Invalid extends LeftProjection[Validated] {
    override def apply(in: Validated[Any, ?]): Yield[Validated[Any, ?], Any, Nothing] = in.fold(some, _ => none)
  }

  object Valid extends RightProjection[Validated] {
    override def apply(in: Validated[?, Any]): Yield[Validated[?, Any], Any, Nothing] = in.fold(_ => none, some)
  }
}
