package h8io.stages.projections

import h8io.stages.Status
import h8io.stages.base.FruitfulStaticStage

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
  object Left extends FruitfulStaticStage[(Any, ?), Any, Nothing] {
    override def produce(in: (Any, ?)): (Any, Status[Nothing]) = (in._1, Status.Success)

    /** Returns a typed view of this projection for first elements of type `T`.
      *
      * The cast is safe because `Tuple2` is covariant in both parameters and the projection only reads (never writes)
      * the pair.
      *
      * @tparam T
      *   the concrete first-element type
      */
    def apply[T]: FruitfulStaticStage[(T, ?), T, Nothing] = asInstanceOf[FruitfulStaticStage[(T, ?), T, Nothing]]
  }

  /** Extracts the second element (`_2`) of a `(A, B)` pair.
    *
    * Use `Tuple2.Right[T]` to get a typed `FruitfulStaticStage[(?, T), T, Nothing]`.
    */
  object Right extends FruitfulStaticStage[(?, Any), Any, Nothing] {
    override def produce(in: (?, Any)): (Any, Status[Nothing]) = (in._2, Status.Success)

    /** Returns a typed view of this projection for second elements of type `T`.
      *
      * @tparam T
      *   the concrete second-element type
      */
    def apply[T]: FruitfulStaticStage[(?, T), T, Nothing] = asInstanceOf[FruitfulStaticStage[(?, T), T, Nothing]]
  }
}
