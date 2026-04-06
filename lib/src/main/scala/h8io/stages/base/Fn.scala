package h8io.stages.base

import h8io.stages.{Status, Yield}

/** A [[Fruitful]] stage backed by a pure function `f`.
  *
  * `Fn` always succeeds (`h8io.stages.Status.Success`) and returns `this` as the `h8io.stages.Evolution` (via
  * [[BaseEvolution]]), making it stateless and reusable. The `apply` implementation is sealed and cannot be overridden;
  * subclasses only need to implement [[f]].
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
trait Fn[-I, +O] extends Fruitful[I, O, Nothing] with BaseStage[I, O, Nothing] {

  /** The pure mapping function applied to each input value.
    *
    * @param in
    *   the input value
    * @return
    *   the computed output value
    */
  def f(in: I): O

  override final def apply(in: I): Yield.Some[I, O, Nothing] = Yield.Some(f(in), Status.Success, this)
}

/** Companion object for [[Fn]]. */
object Fn {

  /** Type alias for an endomorphic [[Fn]] stage (same input and output type, e.g. a transformer). */
  type Endo[T] = Fn[T, T]
}
