package h8io.stages.base

import h8io.stages.{Evolution, Stage, Status}

/** The fruitful counterpart of [[SAMStage]]: a `h8io.stages.Stage.Fruitful` that is its own
  * `h8io.stages.Evolution.Fruitful`.
  *
  * `skip` and `evolve` both return `this`, so every generation is this very instance — which is what carries the
  * always-an-output guarantee forward, as `h8io.stages.Stage.Fruitful` requires of the whole evolution lineage, not
  * just of the current run. Subclasses only implement `apply`.
  *
  * This trait deliberately does not extend [[SAMStage]] or [[Stagnation]]: both seal the methods narrowed here.
  *
  * `dispose` is a no-op by default. Override it when the stage holds external resources that must be released.
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait FruitfulSAMStage[-I, +O, +E] extends Stage.Fruitful[I, O, E] with Evolution.Fruitful[I, O, E] {
  override final def skip(): Evolution.Fruitful[I, O, E] = this

  override final def evolve(status: Status[?]): Stage.Fruitful[I, O, E] = this

  override def dispose(): Unit = ()
}

/** Companion object for [[FruitfulSAMStage]]. */
object FruitfulSAMStage {

  /** Type alias for an endomorphic [[FruitfulSAMStage]] (same input and output type). */
  type Endo[T, +E] = FruitfulSAMStage[T, T, E]
}
