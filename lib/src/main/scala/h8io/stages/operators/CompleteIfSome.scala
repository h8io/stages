package h8io.stages.operators

import h8io.stages.Stage
import h8io.stages.base.Decoration
import h8io.stages.std.Break

/** A [[h8io.stages.base.Decoration]] that stops the pipeline as soon as the decorated stage produces an output value.
  *
  * `CompleteIfSome` appends [[h8io.stages.std.Break]] after the decorated stage via `~>`. If the stage yields
  * `h8io.stages.Yield.Some`, `Break` immediately emits `h8io.stages.Status.Complete`, terminating the pipeline. If the
  * stage yields `h8io.stages.Yield.None`, `Break` is not reached and the pipeline continues normally.
  *
  * The singleton operates on `Stage[Any, Any, Nothing]` and can be cast to any concrete `Decoration[I, O, E]` via
  * `apply[I, O, E]`.
  *
  * Example:
  * {{{
  * val stage: Stage[Int, Int, Nothing] = ...
  * val completing: Stage[Int, Int, Nothing] = CompleteIfSome(stage)
  * // Once stage produces a value, the pipeline stops.
  * }}}
  */
object CompleteIfSome extends Decoration[Any, Any, Nothing] {

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

  override def apply(stage: Stage[Any, Any, Nothing]): Stage[Any, Any, Nothing] = stage ~> Break
}
