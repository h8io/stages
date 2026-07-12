package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.{Decorator, StageOps}

import scala.annotation.tailrec

/** A decorator that feeds the output of each successful step back as the input of the next, effectively looping the
  * inner endomorphic stage until it stops.
  *
  * The tail-recursive loop works as follows:
  *
  *   - `h8io.stages.Status.Success` + `h8io.stages.Yield.Some`: the output becomes the new input and the loop continues
  *     with the stage selected by `yld.evolve()`.
  *   - `h8io.stages.Status.Success` + `h8io.stages.Yield.None`: no output was produced; the loop stops and emits a
  *     `Yield.None` with `Success` status whose continuation is the stage selected by `yld.evolve()`, wrapped back in
  *     `Loop`.
  *   - `h8io.stages.Status.Complete` with no errors: the loop stops, converts the status back to `Success`, and wraps
  *     the stage selected by `yld.evolve()` in `Loop` for the next outer invocation.
  *   - `h8io.stages.Status.Complete` with errors: the loop stops, preserves the errors, and selects the continuation
  *     the same way.
  *
  * In every branch the continuation of the inner stage is selected by the status the inner stage itself produced, and
  * never by the status `Loop` reports to the enclosing pipeline — the two differ, since an error-free `Complete` is
  * absorbed into a `Success`. For the same reason the returned evolution is a `h8io.stages.base.ConstEvolution`: the
  * status the enclosing pipeline evolves `Loop` with belongs to that pipeline, not to the inner stage, and must not be
  * passed inwards. Symmetrically, a skipped `Loop` skips its inner stage and selects the inner continuation on the
  * neutral `Status.Success`. The returned `ConstEvolution` keeps the inner evolution's `dispose` as its own: per the
  * `h8io.stages.Evolution` contract, the inner evolution stays the terminal cleanup handle for the generation
  * constructed here until that generation runs.
  *
  * This makes `Loop` suitable for in-process iterative computations (e.g., fixed-point iterations) where the result of
  * one step seeds the next.
  *
  * @param alterand
  *   the inner endomorphic stage `T → T` to loop
  * @tparam T
  *   the value type (both input and output)
  * @tparam E
  *   the error type (covariant)
  */
final case class Loop[T, +E](alterand: Stage.Endo[T, E]) extends Decorator[T, T, E] {
  override def apply(in: T): Yield[T, T, E] = {
    @tailrec def loop(stage: Stage[T, T, E], in: T): Yield[T, T, E] = {
      val yld = stage(in)
      yld.status match {
        case Status.Success =>
          yld match {
            case Yield.Some(out, _, _) => loop(yld.evolve(), out)
            case Yield.None(_, evolution) =>
              Yield.None(Status.Success, Loop(evolution.evolve(Status.Success)).toEvolution(evolution.dispose _))
          }
        case status: Status.Complete[?] =>
          yld.map(
            identity,
            _ => if (status.isEmpty) Status.Success else status,
            evolution => Loop(evolution.evolve(status)).toEvolution(evolution.dispose _))
      }
    }
    loop(alterand, in)
  }

  override def skip(): Evolution[T, T, E] = {
    val evolution = alterand.skip()
    Loop(evolution.evolve(Status.Success)).toEvolution(evolution.dispose _)
  }
}
