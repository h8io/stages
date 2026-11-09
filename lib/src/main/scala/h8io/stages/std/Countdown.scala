package h8io.stages.std

import h8io.stages
import h8io.stages.base.Fruitful
import h8io.stages.{Stage, Status, Yield}

/** An endomorphic stage that passes its input through for exactly `n` invocations and then signals pipeline completion.
  *
  * On each of the first `n − 1` calls the stage yields `h8io.stages.Status.Success` and decrements its counter. On the
  * `n`-th call it yields `h8io.stages.Status.Complete` and resets to `n`. After any `h8io.stages.Status.Complete` the
  * stage also resets to `n`, ready for reuse — whether it was applied in that run or skipped.
  *
  * `Countdown` is fully immutable: each state transition creates a new instance rather than mutating the current one.
  *
  * Preconditions (checked at construction via `assume`):
  *   - `n > 0`
  *   - `1 ≤ i ≤ n`
  *
  * @param i
  *   the remaining number of successful yields before completion (`1 ≤ i ≤ n`)
  * @param n
  *   the total period length (must be positive)
  * @tparam T
  *   the value type passed through unchanged
  */
final case class Countdown[T](i: Long, n: Long) extends Fruitful.Endo[T, Nothing] {
  assume(n > 0, s"n must be positive, got n = $n")
  assume(0 < i && i <= n, s"i must be in [1, $n], got i = $i")

  override def apply(in: T): Yield.Some[T, T, Nothing] =
    if (i == 1) Yield.Some(in, Status.complete, Countdown.Evolution(Countdown(n, n)))
    else Yield.Some(in, Status.Success, Countdown.Evolution(Countdown(i - 1, n)))

  /** Keeps the counter where it is on `h8io.stages.Status.Success`, and resets it on any other status. */
  override def skip(): stages.Evolution.Endo[T, Nothing] = Countdown.Evolution(this)
}

/** Factory for [[Countdown]] stages. */
object Countdown {

  /** Evolves to `next` on `h8io.stages.Status.Success` and to a fresh `Countdown(n, n)` on any other status. */
  private[std] final case class Evolution[T](next: Countdown[T]) extends stages.Evolution.Endo[T, Nothing] {
    override def evolve(status: Status[?]): Stage.Endo[T, Nothing] =
      status match {
        case Status.Success => next
        case _ => Countdown(next.n, next.n)
      }

    override def dispose(): Unit = ()
  }

  /** Creates a [[Countdown]] that processes `n` items per cycle.
    *
    * If `n ≤ 0`, returns [[DeadEnd]] (which immediately signals completion), ensuring the returned stage is always safe
    * to use.
    *
    * @param n
    *   the cycle length; must be positive for any processing to occur
    * @tparam T
    *   the value type
    * @return
    *   a stage that processes `n` values per cycle, or [[DeadEnd]] if `n ≤ 0`
    */
  def apply[T](n: Long): Stage.Endo[T, Nothing] = if (n > 0) Countdown(n, n) else DeadEnd
}
