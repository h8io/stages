package h8io.stages.std

import h8io.stages.base.Fn

/** A stage that collapses an `Either` by returning whichever side contains the value.
  *
  * `Coalesce` takes an `Either[T, T]` and returns the contained value, regardless of whether it is `Left` or `Right`.
  * It always succeeds (`h8io.stages.Status.Success`) and never changes state, because it is a [[h8io.stages.base.Fn]].
  *
  * Example:
  * {{{
  * val stage: Fn[Either[String, String], String] = Coalesce[String]
  * }}}
  */
object Coalesce extends Fn[Either[Any, Any], Any] {
  override def f(input: Either[Any, Any]): Any =
    input match {
      case Left(value) => value
      case Right(value) => value
    }

  /** Returns a typed view of this singleton as a `Fn[Either[T, T], T]`.
    *
    * @tparam T
    *   the concrete value type on both sides of the `Either`
    */
  def apply[T]: Fn[Either[T, T], T] = asInstanceOf[Fn[Either[T, T], T]]
}
