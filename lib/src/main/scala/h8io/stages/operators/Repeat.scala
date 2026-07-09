package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.{Decorator, StageOps}

import scala.annotation.tailrec

/** A decorator that keeps re-applying the inner stage (following the continuations its `h8io.stages.Evolution` selects
  * for `h8io.stages.Status.Success`) until the stage signals completion or an error.
  *
  * The loop is implemented with `@tailrec` so it does not grow the stack. The loop continues as long as the inner stage
  * yields `h8io.stages.Status.Success`. It stops on `h8io.stages.Status.Complete`: if there are no errors the status is
  * converted back to `Success`; if there are errors, the `Complete` status is preserved.
  *
  * In every branch the continuation of the inner stage is selected by the status the inner stage itself produced, and
  * never by the status `Repeat` reports to the enclosing pipeline — the two differ, since an error-free `Complete` is
  * absorbed into a `Success`. For the same reason the returned evolution is a `h8io.stages.base.ConstEvolution`: the
  * status the enclosing pipeline evolves `Repeat` with belongs to that pipeline, not to the inner stage, and must not
  * be passed inwards. Symmetrically, a skipped `Repeat` skips its inner stage and selects the inner continuation on the
  * neutral `Status.Success`.
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
        case Status.Success => repeat(yld.evolve())
        case status: Status.Complete[?] =>
          yld.map(
            identity,
            _ => if (status.isEmpty) Status.Success else status,
            evolution => Repeat(evolution.evolve(status)).toEvolution(evolution.dispose _))
      }
    }
    repeat(alterand)
  }

  override def skip(): Evolution[I, O, E] = {
    val evolution = alterand.skip()
    Repeat(evolution.evolve(Status.Success)).toEvolution(evolution.dispose _)
  }
}
