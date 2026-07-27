package h8io.stages.base

import h8io.stages.{Stage, Yield}

/** A `Stage` that is guaranteed to always produce a `h8io.stages.Yield.Some` — for ''this'' run only.
  *
  * Overriding `apply` to return `Yield.Some` (instead of the broader `Yield`) lets the compiler track this guarantee
  * statically, making `Fruitful` stages safe to use wherever an output value is always expected.
  *
  * Prefer `h8io.stages.Stage.Fruitful` where the guarantee also holds for every generation the stage evolves into: it
  * narrows `skip` and the evolution as well, so the compiler carries the guarantee along the whole lineage instead of
  * losing it after the first run. This trait remains for the stages that are fruitful now but may evolve into something
  * that is not — `h8io.stages.examples.Cache.Cached`, which drops back to a plain cache on `Complete`, is the
  * motivating example.
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
