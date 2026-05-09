package h8io.stages.projections

import h8io.stages.base.{Projection, StaticYield}

/** A [[h8io.stages.base.Projection]] for `Option[T]`, extracting the contained value when present.
  *
  *   - `Some(v)` → `h8io.stages.Yield.Some``(v, Status.Success, this)`
  *   - `None` → `h8io.stages.Yield.None``(Status.Success, this)`
  *
  * In both cases the status is `h8io.stages.Status.Success`; the absence of a value is not treated as an error.
  *
  * The singleton operates on `Option[Any]` and can be safely cast to `Projection[Option[T], T]` via `apply[T]`.
  */
object Unlift extends Projection[Option[Any], Any] {
  override def process(in: Option[Any]): StaticYield[Any, Nothing] =
    in match {
      case Some(out) => some(out)
      case None => none
    }

  /** Returns a typed view of this singleton as a `Projection[Option[T], T]`.
    *
    * @tparam T
    *   the concrete element type inside the `Option`
    */
  def apply[T]: Projection[Option[T], T] = asInstanceOf[Projection[Option[T], T]]
}
