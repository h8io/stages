package h8io.stages.std

import h8io.stages.base.Fn

/** A stage that swaps the two elements of a pair.
  *
  * `Swap` is a singleton [[h8io.stages.base.Fn]]: given `(a, b)`, it returns `(b, a)`. It always succeeds
  * (`h8io.stages.Status.Success`) and never changes state.
  *
  * The singleton operates on `(Any, Any)` and can be safely cast to any specific pair type via the `apply[L, R]`
  * method, avoiding allocation on every use.
  *
  * `h8io.stages.cycles.Reduce` and `h8io.stages.cycles.Fold` both call `op` in the "left" convention,
  * `op((accumulator, output))`. Composing `Swap[R, O] ~> op` in front of an `op` written in the opposite convention —
  * `(output, accumulator)` — adapts it to the shape `Reduce`/`Fold` expect, without needing a separate
  * `ReduceRight`/`FoldRight` operator: for a non-commutative `op`, this changes which side each value ends up on.
  *
  * Example:
  * {{{
  * val stage: Fn[(Int, String), (String, Int)] = Swap[Int, String]
  * }}}
  */
object Swap extends Fn[(Any, Any), (Any, Any)] {
  override protected def f(in: (Any, Any)): (Any, Any) = in.swap

  /** Returns a typed view of this singleton as a `Fn[(L, R), (R, L)]`.
    *
    * @tparam L
    *   the type of the first element (becomes second)
    * @tparam R
    *   the type of the second element (becomes first)
    */
  def apply[L, R]: Fn[(L, R), (R, L)] = Swap.asInstanceOf[Fn[(L, R), (R, L)]]
}
