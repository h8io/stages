package h8io.xi.stages.std

import h8io.xi.stages.*

import java.time.Duration
import scala.concurrent.duration.FiniteDuration

final case class GlobalSoftDeadline[T](now: () => Long, duration: Long) extends Fruitful.Endo[T, Nothing] {
  private val ts: Long = now()

  def apply(in: T): Yield.Some[T, T, Nothing] =
    Yield.Some(in, if (now() - ts < duration) Signal.Success else Signal.Complete, this)
}

object GlobalSoftDeadline {
  def apply[T](duration: FiniteDuration): Fruitful.Endo[T, Nothing] =
    GlobalSoftDeadline(System.nanoTime _, duration.toNanos)

  def apply[T](duration: Duration): Fruitful.Endo[T, Nothing] = GlobalSoftDeadline(System.nanoTime _, duration.toNanos)
}
