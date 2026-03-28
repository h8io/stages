package h8io.stages.std

import h8io.stages.base.Fn

/** A stage that always produces the same constant output, ignoring its input.
  *
  * `Const` wraps a fixed value `out` and returns it on every invocation, always succeeding
  * (`h8io.stages.Status.Success`). Because it is a [[h8io.stages.base.Fn]], it is stateless and can be reused freely.
  *
  * Example:
  * {{{
  * val always42: Stage[Any, Int, Nothing] = Const(42)
  * }}}
  *
  * @param out
  *   the constant value to produce on every invocation
  * @tparam O
  *   the output type (covariant)
  */
final case class Const[+O](out: O) extends Fn[Any, O] {
  override def f(in: Any): O = out
}
