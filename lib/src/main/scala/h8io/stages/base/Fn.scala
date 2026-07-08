package h8io.stages.base

import h8io.stages.{Status, Yield}

/** The simplest way to create a `Stage` from a single pure function.
  *
  * Extend `Fn` and implement [[f]]; everything else — wrapping in `Yield.Some`, attaching `Status.Success`, wiring the
  * evolution — is handled automatically. `Fn` always succeeds and returns `this` as the evolution (via [[Stagnation]]),
  * making it stateless and reusable.
  *
  * Because `apply` is final, a `Fn` stage can never produce `Yield.None` or a non-`Success` status. When either of
  * those is needed, use [[StaticStage]] instead.
  *
  * Example usage:
  * {{{
  * object DoubleInt extends Fn[Int, Int] {
  *   def f(in: Int): Int = in * 2
  * }
  * }}}
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  */
trait Fn[-I, +O] extends Fruitful[I, O, Nothing] with SAMStage[I, O, Nothing] {
  override final def apply(in: I): Yield.Some[I, O, Nothing] = Yield.Some(f(in), Status.Success, this)

  /** The pure mapping function applied to each input value.
    *
    * @param in
    *   the input value
    * @return
    *   the computed output value
    */
  protected def f(in: I): O
}

/** Companion object for [[Fn]]. */
object Fn {

  /** Type alias for an endomorphic [[Fn]] stage (same input and output type, e.g. a transformer). */
  type Endo[T] = Fn[T, T]
}
