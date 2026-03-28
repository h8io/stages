package h8io.stages.std

import h8io.stages.base.BaseEvolution
import h8io.stages.{Stage, Status, Yield}

/** A stage that extracts the value from an `Option`, forwarding it when present and producing no output when absent.
  *
  *   - `Some(v)` → `h8io.stages.Yield.Some``(v, Status.Success, this)`
  *   - `None` → `h8io.stages.Yield.None``(Status.Success, this)`
  *
  * In both cases the status is `h8io.stages.Status.Success`; the absence of a value is not treated as an error. To stop
  * the pipeline when `None` is encountered, compose with [[h8io.stages.operators.BreakIfNone]].
  *
  * The singleton operates on `Option[Any]` and can be safely cast to `Stage[Option[T], T, Nothing]` via `apply[T]`.
  */
object Unlift extends Stage[Option[Any], Any, Nothing] with BaseEvolution[Option[Any], Any, Nothing] {
  override def apply(in: Option[Any]): Yield[Option[Any], Any, Nothing] =
    in match {
      case Some(out) => Yield.Some(out, Status.Success, this)
      case None => Yield.None(Status.Success, this)
    }

  /** Returns a typed view of this singleton as a `Stage[Option[T], T, Nothing]`.
    *
    * @tparam T
    *   the concrete element type inside the `Option`
    */
  def apply[T]: Stage[Option[T], T, Nothing] = asInstanceOf[Stage[Option[T], T, Nothing]]
}
