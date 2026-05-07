package h8io.stages.projections

import h8io.stages.base.{LeftProjection, RightProjection, StaticYield}

/** Stage projections for Scala's standard `scala.util.Either` type.
  *
  * Each projection extracts one side of an `Either` and either passes the value downstream (`h8io.stages.Yield.Some`)
  * or produces no output (`h8io.stages.Yield.None`) when the `Either` holds the other side. In both cases the status is
  * `h8io.stages.Status.Success`, so the pipeline continues.
  *
  * Example:
  * {{{
  * val leftStage  = Either.Left[String]   // Stage[scala.util.Either[String, ?], String, Nothing]
  * val rightStage = Either.Right[Int]     // Stage[scala.util.Either[?, Int], Int, Nothing]
  * }}}
  */
object Either {

  /** Extracts the left value of an `Either[A, B]`, producing no output for `Right` values.
    *
    * Use `Either.Left[T]` to get a typed `Projection[scala.util.Either[T, ?], T]`.
    */
  object Left extends LeftProjection[Either] {
    override def process(in: Either[Any, ?]): StaticYield[Any, Nothing] = in.fold(some, _ => none)
  }

  /** Extracts the right value of an `Either[A, B]`, producing no output for `Left` values.
    *
    * Use `Either.Right[T]` to get a typed `Projection[scala.util.Either[?, T], T]`.
    */
  object Right extends RightProjection[Either] {
    override def process(in: Either[?, Any]): StaticYield[Any, Nothing] = in.fold(_ => none, some)
  }
}
