package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.{Decorator, StageOps}

import scala.annotation.tailrec

/** A decorator that keeps re-applying the inner stage (following its `h8io.stages.Evolution.onSuccess` transitions)
  * until the stage signals completion or an error.
  *
  * The loop is implemented with `@tailrec` so it does not grow the stack. The loop continues as long as the inner stage
  * yields `h8io.stages.Status.Success`; it stops on `h8io.stages.Status.Complete` (converting the status to `Success`
  * and selecting the `onComplete` continuation) or on `h8io.stages.Status.Error` (preserving the error and selecting
  * `onError`).
  *
  * `Repeat` is useful for driving stages like [[h8io.stages.std.Countdown]] that signal "batch complete" via
  * `Status.Complete` and should be repeated as a whole unit.
  *
  * @param alterand
  *   the inner stage to repeat
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class Repeat[-I, +O, +E](alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = {
    @tailrec def repeat(stage: Stage[I, O, E]): Yield[I, O, E] = {
      val yld = stage(in)
      yld.status match {
        case Status.Success => repeat(yld.evolution.onSuccess())
        case Status.Complete =>
          yld.map(identity, _ => Status.Success, evolution => Repeat(evolution.onComplete()).toEvolution)
        case _: Status.Error[E] => yld.map(identity, identity, evolution => Repeat(evolution.onError()).toEvolution)
      }
    }
    repeat(alterand)
  }

  override def skip(): Evolution[I, O, E] = alterand.skip().map(Repeat(_))
}
