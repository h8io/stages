package h8io.stages.base

import h8io.stages.{Stage, Yield}

/** A `Stage` that is guaranteed to always produce a `h8io.stages.Yield.Some`.
  *
  * Overriding `apply` to return `Yield.Some` (instead of the broader `Yield`) lets the compiler track this guarantee
  * statically, making `Fruitful` stages safe to use wherever an output value is always expected.
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait Fruitful[-I, +O, +E] extends Stage[I, O, E] {
  override def apply(in: I): Yield.Some[I, O, E]
}

/** Companion object for [[Fruitful]]. */
object Fruitful {

  /** Type alias for an endomorphic [[Fruitful]] stage (same input and output type). */
  type Endo[T, +E] = Fruitful[T, T, E]
}
