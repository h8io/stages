package h8io.stages.operators

import h8io.stages.Stage
import h8io.stages.base.Decoration

/** The identity [[h8io.stages.base.Decoration]]: returns the decorated stage unchanged.
  *
  * `Identity` is useful as a no-op placeholder in decoration pipelines or when a [[h8io.stages.base.Decoration]] is
  * required by an API but no transformation is needed.
  *
  * The singleton operates on `Stage[Any, Nothing, Nothing]` and can be safely cast to any concrete
  * `Decoration[I, O, E]` via `apply[I, O, E]`.
  *
  * Example:
  * {{{
  * val dec: Decoration[String, Int, Nothing] = Identity[String, Int, Nothing]
  * }}}
  */
object Identity extends Decoration[Any, Nothing, Nothing] {

  /** Returns a typed view of this singleton as a `Decoration[I, O, E]`.
    *
    * @tparam I
    *   the input type
    * @tparam O
    *   the output type
    * @tparam E
    *   the error type
    */
  def apply[I, O, E]: Decoration[I, O, E] = asInstanceOf[Decoration[I, O, E]]

  override def apply(stage: Stage[Any, Nothing, Nothing]): Stage[Any, Nothing, Nothing] = stage
}
