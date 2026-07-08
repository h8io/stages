package h8io.stages.base

import h8io.stages.{Status, Yield}

/** A `h8io.stages.Stage` that is its own `Evolution` and is guaranteed to always produce an output value.
  *
  * The fruitful counterpart of [[StaticStage]]: `apply` and `skip` are sealed, and subclasses implement [[process]],
  * returning the output value paired with its `h8io.stages.Status` — no yield wrapping at all. `apply` carries the
  * `h8io.stages.Yield.Some` return type required by [[Fruitful]], so the always-an-output guarantee is statically
  * tracked.
  *
  * See [[Fn]] for the further-constrained variant that, in addition, always succeeds.
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait FruitfulStaticStage[-I, +O, +E] extends SAMStage[I, O, E] with Fruitful[I, O, E] {
  override final def apply(in: I): Yield.Some[I, O, E] = {
    val (out, status) = process(in)
    Yield.Some(out, status, this)
  }

  /** The processing logic for this stage: computes the output value and the resulting `h8io.stages.Status` for the
    * given input.
    *
    * @param in
    *   the input value
    * @return
    *   the output value paired with the execution status
    */
  protected def process(in: I): (O, Status[E])
}

/** Companion object for [[FruitfulStaticStage]]. */
object FruitfulStaticStage {

  /** Type alias for an endomorphic [[FruitfulStaticStage]] (same input and output type). */
  type Endo[T, +E] = FruitfulStaticStage[T, T, E]
}
