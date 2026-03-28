package h8io.stages.cats

import h8io.stages.Yield
import h8io.stages.base.{LeftProjection, RightProjection}

/** Stage projections for `cats.data.Validated`.
  *
  * `cats.data.Validated[L, R]` is a right-biased validation type where `Invalid(l)` holds an error value and `Valid(r)`
  * holds a success value. The two projections here allow a pipeline to route values based on which side is present,
  * producing `h8io.stages.Yield.Some` for the expected side and `h8io.stages.Yield.None` otherwise. In both cases the
  * status is `h8io.stages.Status.Success`.
  *
  * Example:
  * {{{
  * import h8io.stages.cats.Validated
  *
  * val validStage   = Validated.Valid[String]    // Stage[cats.data.Validated[?, String], String, Nothing]
  * val invalidStage = Validated.Invalid[String]  // Stage[cats.data.Validated[String, ?], String, Nothing]
  * }}}
  */
object Validated {
  type Validated[+L, +R] = cats.data.Validated[L, R]

  /** Extracts the `Invalid` (left/error) value, yielding nothing for `Valid` values.
    *
    * Use `Validated.Invalid[T]` to get a typed `Projection[Validated[T, ?], T]`.
    */
  object Invalid extends LeftProjection[Validated] {
    override def apply(in: Validated[Any, ?]): Yield[Validated[Any, ?], Any, Nothing] = in.fold(some, _ => none)
  }

  /** Extracts the `Valid` (right/success) value, yielding nothing for `Invalid` values.
    *
    * Use `Validated.Valid[T]` to get a typed `Projection[Validated[?, T], T]`.
    */
  object Valid extends RightProjection[Validated] {
    override def apply(in: Validated[?, Any]): Yield[Validated[?, Any], Any, Nothing] = in.fold(_ => none, some)
  }
}
