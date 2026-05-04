package h8io.stages.std

import h8io.stages.base.{BaseStage, Fruitful, StageOps}
import h8io.stages.{Status, Yield}

/** A stage that immediately signals pipeline completion without dropping the input value.
  *
  * `Break` always yields `h8io.stages.Status.Complete` while still passing the input through as the output. It is
  * useful as the terminal element in a pipeline to stop further processing after a condition is met (see
  * [[h8io.stages.operators.BreakIfSome]]).
  *
  * The singleton operates on `Any` and can be safely cast to any `Fruitful.Endo[T]` via `apply[T]`.
  */
object Break extends Fruitful.Endo[Any, Nothing] with BaseStage.Endo[Any, Nothing] {

  /** Returns a typed view of this singleton as a `Fruitful.Endo[T]`.
    *
    * @tparam T
    *   the concrete value type
    */
  def apply[T]: Fruitful.Endo[T, Nothing] = asInstanceOf[Fruitful.Endo[T, Nothing]]

  override def apply(in: Any): Yield.Some[Any, Any, Nothing] = Yield.Some(in, Status.complete, this.toEvolution)
}
