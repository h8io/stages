package h8io.stages.cycles

import h8io.stages.base.{Alterator, ConstEvolution}
import h8io.stages.{Evolution, Stage, Status, Yield}

import scala.annotation.tailrec

/** A decorator that drives a whole cycle of the inner stage per outer run — like [[Repeat]] and [[Reduce]], on the same
  * outer input — folding every output the inner stage produces into an accumulator `R` via the binary operation stage
  * `op`.
  *
  * Unlike [[Reduce]], the accumulator is never empty: `apply` receives the seed `R` alongside the input, so every
  * output the inner stage produces — including the first — is folded via `op((accumulator, output))`; there is no
  * seedless first output to special-case. Because the accumulator always has a value to yield, `Fold` mixes in
  * `h8io.stages.Stage.Fruitful`: `apply` is statically guaranteed to return `h8io.stages.Yield.Some`, and so is every
  * generation it evolves into.
  *
  * That guarantee is what makes `op` a `h8io.stages.Stage.Fruitful`: a fold that produced no value would leave the
  * accumulator with nothing to become, and a fruitful `Fold` has no `None` to fall back to. Requiring the guarantee of
  * `op` up front is what removes the case rather than papering over it — there is no unfolded output to discard or to
  * carry over. When the inner stage itself produces no output there is nothing to fold, so `op` is skipped — as in
  * [[Reduce]] — and only the inner stage's status carries the iteration; each iteration therefore still performs
  * exactly one of `apply`/`skip` on both inner stages, as the lifecycle contract requires. Such an iteration keeps the
  * cycle spinning as long as the combined status is `h8io.stages.Status.Success`, like [[Repeat]]. The cycle is
  * `@tailrec` and does not grow the stack.
  *
  * The cycle stops when the status of an iteration is `h8io.stages.Status.Complete`: an error-free `Complete` is
  * reported to the enclosing pipeline as `Success`, a `Complete` with errors is preserved. The accumulator as it stands
  * at that point is yielded. The accumulator is local to one outer run: the next generation of `Fold` starts fresh from
  * whatever seed the next outer input supplies.
  *
  * ==The inner pipeline==
  *
  * `alterand` and `op` together form the inner pipeline of `Fold`, with `op` downstream of `alterand`. An iteration's
  * status is therefore the combination of their statuses in pipeline order (`alterand` first), and both continuations
  * are selected by that combined status — exactly as `h8io.stages.Evolution.AndThen` evolves both sides of a composed
  * pipeline with the status of the whole run, never with the stages' individual statuses.
  *
  * As in [[Loop]] and [[Repeat]], the continuations are never selected by the status `Fold` reports to the enclosing
  * pipeline — the two differ, since an error-free `Complete` is absorbed into a `Success` — and for the same reason the
  * returned evolution is a `h8io.stages.base.ConstEvolution.Fruitful`. Its `dispose` releases both inner evolutions via
  * `h8io.stages.Evolution.dispose` (`op` first, then `alterand`), which per the `h8io.stages.Evolution` contract stay
  * the terminal cleanup handles of the generation just constructed. Symmetrically a skipped `Fold` skips both inner
  * stages and selects their continuations on the neutral `Status.Success`.
  *
  * @param alterand
  *   the inner stage whose outputs are folded
  * @param op
  *   the binary operation stage folding the accumulator and an output into the next accumulator; fruitful, since every
  *   fold must yield the next accumulator
  * @tparam R
  *   the accumulator type (invariant: seeded via the outer input, threaded as both input and output of `op`)
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the value type produced by `alterand` and consumed alongside the accumulator by `op` (invariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class Fold[R, -I, O, +E](alterand: Stage[I, O, E], op: Stage.Fruitful[(R, O), R, E])
    extends Alterator[Stage[I, O, E], (R, I), R, E] with Stage.Fruitful[(R, I), R, E] {
  override def apply(in: (R, I)): Yield.Some.Fruitful[(R, I), R, E] = Fold.execute(in._2, in._1, alterand, op)

  override def skip(): Evolution.Fruitful[(R, I), R, E] = Fold.evolve(Status.Success, alterand.skip(), op.skip())
}

object Fold {
  @tailrec private def execute[R, I, O, E](in: I, value: R, alterand: Stage[I, O, E], op: Stage.Fruitful[(R, O), R, E])
      : Yield.Some.Fruitful[(R, I), R, E] = {
    val Yield(optAlterandOut, alterandStatus, alterandEvolution) = alterand(in)
    val (result, status, opEvolution) = optAlterandOut match {
      case Some(alterandOut) =>
        val Yield.Some.Fruitful(out, opStatus, opEvolution) = op((value, alterandOut))
        (out, alterandStatus.combine(opStatus), opEvolution)
      case None => (value, alterandStatus, op.skip())
    }
    status match {
      case Status.Success =>
        val evolvedOp = opEvolution.evolve(status)
        val evolvedAlterand = alterandEvolution.evolve(status)
        execute(in, result, evolvedAlterand, evolvedOp)
      case complete: Status.Complete[E] =>
        Yield.Some.Fruitful(
          result, if (complete.isEmpty) Status.Success else complete, evolve(status, alterandEvolution, opEvolution))
    }
  }

  private def evolve[R, I, O, E](
      status: Status[E],
      alterandEvolution: Evolution[I, O, E],
      opEvolution: Evolution.Fruitful[(R, O), R, E]): Evolution.Fruitful[(R, I), R, E] = {
    val evolvedOp = opEvolution.evolve(status)
    val evolvedAlterand = alterandEvolution.evolve(status)
    ConstEvolution.Fruitful(Fold(evolvedAlterand, evolvedOp), () => Evolution.dispose(opEvolution, alterandEvolution))
  }
}
