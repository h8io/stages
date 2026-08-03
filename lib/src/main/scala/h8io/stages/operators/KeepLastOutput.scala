package h8io.stages.operators

import h8io.stages.base.{Decorator, Fruitful}
import h8io.stages.{Evolution, Stage, Yield}

/** A decorator that remembers the last output produced by the inner stage and re-emits it when the inner stage yields
  * no value.
  *
  * The decorator has two internal states:
  *
  *   - **`None` state** (initial): no output has been seen yet. The inner stage's `h8io.stages.Yield.None` is forwarded
  *     unchanged; a `h8io.stages.Yield.Some` is forwarded and transitions the wrapper to the `Some` state.
  *   - **`Some` state**: a previous output `out` is remembered, so the wrapper always emits a value. A new
  *     `h8io.stages.Yield.Some` from the inner stage is forwarded as is and updates the remembered value; on
  *     `h8io.stages.Yield.None` the remembered value is emitted instead.
  *
  * In both states the evolution is mapped so that continuations remain wrapped in the appropriate `KeepLastOutput`
  * variant, preserving the last-value semantics.
  *
  * Both variants are internal to the package: the decorator is reached through the `KeepLastOutput.apply` factory,
  * which always starts in the `None` state, and the state it is in afterwards is not part of the API.
  */
object KeepLastOutput {
  private[operators] final case class None[-I, +O, +E](alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
    override def apply(in: I): Yield[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, status, evolution) => Yield.Some(out, status, evolution.map(Some(out, _)))
        case Yield.None(status, evolution) => Yield.None(status, evolution.map(None(_)))
      }

    override def skip(): Evolution[I, O, E] = alterand.skip().map(None(_))
  }

  private[operators] final case class Some[-I, +O, +E](out: O, alterand: Stage[I, O, E])
      extends Decorator[I, O, E] with Fruitful[I, O, E] {
    override def apply(in: I): Yield.Some[I, O, E] =
      alterand(in) match {
        case Yield.Some(out, status, evolution) => Yield.Some(out, status, evolution.map(Some(out, _)))
        case Yield.None(status, evolution) => Yield.Some(out, status, evolution.map(Some(out, _)))
      }

    override def skip(): Evolution[I, O, E] = alterand.skip().map(Some(out, _))
  }

  /** Wraps `stage` in a `KeepLastOutput` starting in the initial (no remembered value) state.
    *
    * @param stage
    *   the stage to wrap
    * @tparam I
    *   the input type
    * @tparam O
    *   the output type
    * @tparam E
    *   the error type
    * @return
    *   a stage that re-emits the last seen output when the inner stage yields nothing
    */
  def apply[I, O, E](stage: Stage[I, O, E]): Stage[I, O, E] = None(stage)
}
