package h8io.stages.std

import h8io.stages.base.Fn

/** A stage that passes its input through unchanged.
  *
  * `Identity` is a singleton [[h8io.stages.base.Fn]] that acts as the identity function: given any value, it returns
  * the same value. It always succeeds (`h8io.stages.Status.Success`) and never changes state.
  *
  * The singleton operates on `Any` and can be safely cast to any specific `Fn.Endo[T]` via the `apply[T]` method,
  * avoiding allocation on every use.
  *
  * Example:
  * {{{
  * val stage: Fn.Endo[String] = Identity[String]
  * }}}
  */
object Identity extends Fn.Endo[Any] {

  /** Returns a typed view of this singleton as a `Fn.Endo[T]`.
    *
    * @tparam T
    *   the concrete value type
    */
  def apply[T]: Fn.Endo[T] = asInstanceOf[Fn.Endo[T]]

  override def f(in: Any): Any = in
}
