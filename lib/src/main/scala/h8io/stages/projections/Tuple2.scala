package h8io.stages.projections

import h8io.stages.base.{LeftProjection, RightProjection, StaticYield}

/** Stage projections for Scala's standard `scala.Tuple2` type.
  *
  * Each projection extracts one element of a pair `(A, B)` and always yields `h8io.stages.Yield.Some` with
  * `h8io.stages.Status.Success`, because a tuple always has both elements. For that reason the projections are
  * [[h8io.stages.base.FruitfulStaticStage]]s rather than plain `h8io.stages.base.Projection`s: the always-an-output
  * guarantee is visible in their types.
  *
  * Example:
  * {{{
  * val fstStage = Tuple2.Left[String]   // FruitfulStaticStage[(String, ?), String, Nothing]
  * val sndStage = Tuple2.Right[Int]     // FruitfulStaticStage[(?, Int), Int, Nothing]
  * }}}
  */
object Tuple2 {

  /** Extracts the first element (`_1`) of a `(A, B)` pair.
    *
    * Use `Tuple2.Left[T]` to get a typed `FruitfulStaticStage[(T, ?), T, Nothing]`.
    */
  object Left extends LeftProjection[Tuple2] {
    override def process(in: (Any, ?)): StaticYield.Some[Any, Nothing] = some(in._1)
  }

  /** Extracts the second element (`_2`) of a `(A, B)` pair.
    *
    * Use `Tuple2.Right[T]` to get a typed `FruitfulStaticStage[(?, T), T, Nothing]`.
    */
  object Right extends RightProjection[Tuple2] {
    override def process(in: (?, Any)): StaticYield.Some[Any, Nothing] = some(in._2)
  }
}
