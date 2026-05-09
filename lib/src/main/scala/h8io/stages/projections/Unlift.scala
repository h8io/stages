package h8io.stages.projections

import h8io.stages.Stage
import h8io.stages.base.{Projection, StaticYield}

/** A stage that extracts the value from an `Option`, forwarding it when present and producing no output when absent.
  *
  *   - `Some(v)` → `h8io.stages.Yield.Some``(v, Status.Success, this)`
  *   - `None` → `h8io.stages.Yield.None``(Status.Success, this)`
  *
  * In both cases the status is `h8io.stages.Status.Success`; the absence of a value is not treated as an error.
  *
  * The singleton operates on `Option[Any]` and can be safely cast to `Stage[Option[T], T, Nothing]` via `apply[T]`.
  */
object Unlift extends Projection[Option[Any], Any] {
  override def process(in: Option[Any]): StaticYield[Any, Nothing] =
    in match {
      case Some(out) => some(out)
      case None => none
    }

  /** Returns a typed view of this singleton as a `Stage[Option[T], T, Nothing]`.
    *
    * @tparam T
    *   the concrete element type inside the `Option`
    */
  def apply[T]: Stage[Option[T], T, Nothing] = asInstanceOf[Stage[Option[T], T, Nothing]]
}
