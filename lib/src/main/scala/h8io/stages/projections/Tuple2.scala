package h8io.stages.projections

import h8io.stages.base.{LeftProjection, RightProjection, StaticYield}

/** Stage projections for Scala's standard `scala.Tuple2` type.
  *
  * Each projection extracts one element of a pair `(A, B)` and always yields `h8io.stages.Yield.Some` with
  * `h8io.stages.Status.Success`, because a tuple always has both elements.
  *
  * Example:
  * {{{
  * val fstStage = Tuple2.Left[String]   // Stage[(String, ?), String, Nothing]
  * val sndStage = Tuple2.Right[Int]     // Stage[(?, Int), Int, Nothing]
  * }}}
  */
object Tuple2 {

  /** Extracts the first element (`_1`) of a `(A, B)` pair.
    *
    * Use `Tuple2.Left[T]` to get a typed `Projection[(T, ?), T]`.
    */
  object Left extends LeftProjection[Tuple2] {
    override def process(in: (Any, ?)): StaticYield.Some[Any, Nothing] = some(in._1)
  }

  /** Extracts the second element (`_2`) of a `(A, B)` pair.
    *
    * Use `Tuple2.Right[T]` to get a typed `Projection[(?, T), T]`.
    */
  object Right extends RightProjection[Tuple2] {
    override def process(in: (?, Any)): StaticYield.Some[Any, Nothing] = some(in._2)
  }
}
