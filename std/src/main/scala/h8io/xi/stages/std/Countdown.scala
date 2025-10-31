package h8io.xi.stages.std

import h8io.xi.stages.{Signal, Stage, Yield}

final case class Countdown[T](i: Long, n: Long) extends Fruitful.Endo[T, Nothing] {
  assume(n > 0, s"n must be positive, got n = $n")
  assume(0 < i && i <= n, s"i must be in [1, $n], got i = $i")

  def apply(in: T): Yield.Some[T, T, Nothing] =
    if (i == 1) Yield.Some(in, Signal.Complete, Countdown(n, n))
    else Yield.Some(in, Signal.Success, this)

  override def onSuccess(): Stage[T, T, Nothing] = Countdown(i - 1, n)
  override def onComplete(): Stage[T, T, Nothing] = Countdown(n, n)
  override def onError(): Stage[T, T, Nothing] = Countdown(n, n)
}

object Countdown {
  def apply[T](n: Long): Stage.Endo[T, Nothing] = if (n > 0) Countdown(n, n) else DeadEnd
}
