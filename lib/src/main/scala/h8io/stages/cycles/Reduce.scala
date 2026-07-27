package h8io.stages.cycles

import h8io.stages.base.{Decorator, StageOps}
import h8io.stages.{Evolution, Stage, Status, Yield}

import scala.annotation.tailrec

/** A decorator that drives a whole cycle of the inner stage per outer run — like [[Repeat]], on the same outer input —
  * folding every output the inner stage produces into an accumulator with the binary operation stage `op`.
  *
  * The first output becomes the accumulator without invoking `op` (the classic seedless reduce); each subsequent output
  * is folded via `op((accumulator, output))`. In an iteration with nothing to fold — the inner stage produced no
  * output, or the output is the first one — `op` is skipped, so each iteration performs exactly one of `apply`/`skip`
  * on both inner stages, as the lifecycle contract requires. An iteration yielding no output with
  * `h8io.stages.Status.Success` keeps the cycle spinning, like [[Repeat]]. The cycle is `@tailrec` and does not grow
  * the stack.
  *
  * `op` is a `h8io.stages.Stage.Fruitful`: every fold it is applied to yields the next accumulator, so there is no
  * unfolded output to discard or to carry over, exactly as in [[Fold]].
  *
  * The cycle stops when the status of an iteration is `h8io.stages.Status.Complete`: an error-free `Complete` is
  * reported to the enclosing pipeline as `Success`, a `Complete` with errors is preserved. The value accumulated so
  * far, if any, is yielded. The accumulator is local to one outer run: the next generation of `Reduce` starts empty.
  *
  * ==The inner pipeline==
  *
  * `alterand` and `op` together form the inner pipeline of `Reduce`, with `op` downstream of `alterand`. An iteration's
  * status is therefore the combination of their statuses in pipeline order (`alterand` first), and both continuations
  * are selected by that combined status — exactly as `h8io.stages.Evolution.AndThen` evolves both sides of a composed
  * pipeline with the status of the whole run, never with the stages' individual statuses.
  *
  * As in [[Loop]] and [[Repeat]], the continuations are never selected by the status `Reduce` reports to the enclosing
  * pipeline — the two differ, since an error-free `Complete` is absorbed into a `Success` — and for the same reason the
  * returned evolution is a `h8io.stages.base.ConstEvolution`. Its `dispose` releases both inner evolutions via
  * `h8io.stages.Evolution.dispose` (`op` first, then `alterand`), which per the `h8io.stages.Evolution` contract stay
  * the terminal cleanup handles of the generation just constructed. Symmetrically a skipped `Reduce` skips both inner
  * stages and selects their continuations on the neutral `Status.Success`.
  *
  * @param alterand
  *   the inner stage whose outputs are reduced
  * @param op
  *   the binary operation stage folding two values into one; fruitful, since every fold must yield the next accumulator
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the value type being reduced (invariant: produced by `alterand`, consumed in pairs by `op`)
  * @tparam E
  *   the error type (covariant)
  */
final case class Reduce[-I, O, +E](alterand: Stage[I, O, E], op: Stage.Fruitful[(O, O), O, E])
    extends Decorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = Reduce.execute(in, None, alterand, op)

  override def skip(): Evolution[I, O, E] = Reduce.evolve(Status.Success, alterand.skip(), op.skip())
}

object Reduce {
  @tailrec private def execute[I, O, E](
      in: I,
      optValue: Option[O],
      alterand: Stage[I, O, E],
      op: Stage.Fruitful[(O, O), O, E]): Yield[I, O, E] = {
    val Yield(optAlterandOut, alterandStatus, alterandEvolution) = alterand(in)
    val (result, status, opEvolution) = optAlterandOut match {
      case Some(alterandOut) =>
        optValue match {
          case Some(value) =>
            val Yield.Some.Fruitful(out, opStatus, opEvolution) = op((value, alterandOut))
            (Some(out), alterandStatus.combine(opStatus), opEvolution)
          case None => (optAlterandOut, alterandStatus, op.skip())
        }
      case None => (optValue, alterandStatus, op.skip())
    }
    status match {
      case Status.Success =>
        val evolvedOp = opEvolution.evolve(status)
        val evolvedAlterand = alterandEvolution.evolve(status)
        execute(in, result, evolvedAlterand, evolvedOp)
      case complete: Status.Complete[E] =>
        Yield(
          result, if (complete.isEmpty) Status.Success else complete, evolve(status, alterandEvolution, opEvolution))
    }
  }

  private def evolve[I, O, E](
      status: Status[E],
      alterandEvolution: Evolution[I, O, E],
      opEvolution: Evolution.Fruitful[(O, O), O, E]): Evolution[I, O, E] = {
    val evolvedOp = opEvolution.evolve(status)
    val evolvedAlterand = alterandEvolution.evolve(status)
    Reduce(evolvedAlterand, evolvedOp).toEvolution(() => Evolution.dispose(opEvolution, alterandEvolution))
  }
}
