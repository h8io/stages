package h8io.stages.std

import h8io.stages.base.{BaseStage, Fruitful}
import h8io.stages.{Stage, Status, Yield}

/** An endomorphic stage that passes its input through for exactly `n` invocations and then signals pipeline completion.
  *
  * On each of the first `n − 1` calls the stage yields `h8io.stages.Status.Success` and decrements its counter. On the
  * `n`-th call it yields `h8io.stages.Status.Complete` and resets to `n`. After a `h8io.stages.Status.Complete` or
  * `h8io.stages.Status.Error` the stage also resets to `n`, ready for reuse.
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
final case class Countdown[T](i: Long, n: Long) extends Fruitful.Endo[T, Nothing] with BaseStage.Endo[T, Nothing] {
  assume(n > 0, s"n must be positive, got n = $n")
  assume(0 < i && i <= n, s"i must be in [1, $n], got i = $i")

  override def apply(in: T): Yield.Some[T, T, Nothing] =
    if (i == 1) Yield.Some(in, Status.Complete, Countdown(n, n))
    else Yield.Some(in, Status.Success, this)

  override def onSuccess(): Stage[T, T, Nothing] = Countdown(i - 1, n)
  override def onComplete(): Stage[T, T, Nothing] = Countdown(n, n)
  override def onError(): Stage[T, T, Nothing] = Countdown(n, n)
}

/** Factory for [[Countdown]] stages. */
object Countdown {

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
