package h8io.stages.projections

import h8io.stages.Yield

object Tuple2 {
  object Left extends LeftProjection[Tuple2] {
    override def apply(in: (Any, ?)): Yield.Some[(Any, ?), Any, Nothing] = some(in._1)
  }

  object Right extends RightProjection[Tuple2] {
    override def apply(in: (?, Any)): Yield.Some[(?, Any), Any, Nothing] = some(in._2)
  }
}
