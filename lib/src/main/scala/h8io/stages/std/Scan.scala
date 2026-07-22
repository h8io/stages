package h8io.stages.std

import h8io.stages
import h8io.stages.{Stage, Status, Yield}

/** An endomorphic stage that folds its own inputs into a running accumulator across pipeline runs, via the binary
  * operation stage `op` — the running-total counterpart of `h8io.stages.cycles.Reduce` and `h8io.stages.cycles.Fold`.
  *
  * Unlike the `h8io.stages.cycles` family, `Scan` has no inner stage and no cycle: it consumes one external input per
  * outer run and threads its own accumulator from one run to the next. The first input becomes the accumulator
  * unchanged, without invoking `op` (the classic seedless reduce, seeded from the pipeline's own input stream instead
  * of a single seed value); each subsequent input is folded via `op((accumulator, input))` and the result — the new
  * running total — is emitted immediately, ready for the next run.
  *
  * When `op` is applied but itself yields no output — e.g. it filters some inputs out — the accumulator is left
  * unchanged rather than discarded, exactly as in `Reduce`/`Fold`, and `Scan` itself yields nothing for that run.
  *
  * Since `Scan` has no separate alterand, the status of a run is simply the status `op` produced (or `Success`, for the
  * seeding run in which `op` is skipped) — there is nothing else to combine it with. Whenever that status is
  * `h8io.stages.Status.Complete`, the next generation resets to a fresh, unseeded `Scan` instead of continuing to
  * accumulate — mirroring how `h8io.stages.std.Countdown` resets on any non-`Success` status: `Complete` marks one
  * logical unit of work as finished, and the next external input starts a new one, seeding from scratch.
  *
  * @param op
  *   the binary operation stage folding the accumulator and a new input into the next accumulator
  * @tparam T
  *   the accumulator and input type (invariant: threaded through as both input and output of `op`)
  * @tparam E
  *   the error type (covariant)
  */
final case class Scan[T, +E](op: Stage[(T, T), T, E]) extends Stage.Endo[T, E] {
  override def apply(in: T): Yield[T, T, E] = Yield.Some(in, Status.Success, Scan.Evolution(in, op.skip()))

  override def skip(): stages.Evolution[T, T, E] = op.skip().map(Scan(_))
}

object Scan {

  /** The evolution shared by every generation of `Scan`, seeded or not: on `Status.Success` it carries `value` forward
    * into the next `Initialized` generation; on `Status.Complete` it discards `value` and resets to a fresh, unseeded
    * `Scan`, ready to be seeded again from whatever input the next run supplies.
    */
  private case class Evolution[T, +E](value: T, opEvolution: stages.Evolution[(T, T), T, E])
      extends stages.Evolution.Endo[T, E] {
    override def evolve(status: Status[?]): Stage[T, T, E] =
      status match {
        case Status.Success => Initialized(value, opEvolution.evolve(status))
        case _: Status.Complete[?] => Scan(opEvolution.evolve(status))
      }

    override def dispose(): Unit = opEvolution.dispose()
  }

  private case class Initialized[T, E](value: T, op: Stage[(T, T), T, E]) extends Stage.Endo[T, E] {
    override def apply(in: T): Yield[T, T, E] =
      op((value, in)) match {
        case Yield.Some(out, status, opEvolution) => Yield.Some(out, status, Evolution(out, opEvolution))
        case Yield.None(status, opEvolution) => Yield.None(status, Evolution(value, opEvolution))
      }

    override def skip(): stages.Evolution[T, T, E] = Evolution(value, op.skip())
  }
}
