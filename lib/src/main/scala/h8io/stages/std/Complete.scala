package h8io.stages.std

import h8io.stages.Status
import h8io.stages.base.{Fruitful, FruitfulStaticStage}

/** A stage that immediately signals pipeline completion without dropping the input value.
  *
  * `Complete` always yields `h8io.stages.Status.Complete` while still passing the input through as the output. It is
  * useful as the terminal element in a pipeline to stop further processing after a condition is met (see
  * [[h8io.stages.operators.CompleteIfSome]]).
  *
  * The singleton operates on `Any` and can be safely cast to any `Fruitful.Endo[T]` via `apply[T]`.
  */
object Complete extends FruitfulStaticStage.Endo[Any, Nothing] {

  /** Returns a typed view of this singleton as a `Fruitful.Endo[T]`.
    *
    * @tparam T
    *   the concrete value type
    */
  def apply[T]: Fruitful.Endo[T, Nothing] = asInstanceOf[Fruitful.Endo[T, Nothing]]

  override protected def process(in: Any): (Any, Status[Nothing]) = (in, Status.complete)
}
