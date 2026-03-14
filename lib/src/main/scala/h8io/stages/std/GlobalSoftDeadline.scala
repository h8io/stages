package h8io.stages.std

import h8io.stages.*
import h8io.stages.base.{BaseEvolution, Fruitful}

import java.time.Duration
import scala.concurrent.duration.FiniteDuration

final case class GlobalSoftDeadline[T](now: () => Long, duration: Long)
    extends Fruitful.Endo[T, Nothing] with BaseEvolution.Endo[T, Nothing] {
  private val ts: Long = now()

  override def apply(in: T): Yield.Some[T, T, Nothing] =
    Yield.Some(in, if (now() - ts < duration) Status.Success else Status.Complete, this)
}

object GlobalSoftDeadline {
  def apply[T](duration: FiniteDuration): Fruitful.Endo[T, Nothing] =
    GlobalSoftDeadline(System.nanoTime _, duration.toNanos)

  def apply[T](duration: Duration): Fruitful.Endo[T, Nothing] = GlobalSoftDeadline(System.nanoTime _, duration.toNanos)
}
