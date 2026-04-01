package h8io.stages.std

import h8io.stages.*
import h8io.stages.base.{BaseEvolution, Fruitful}

import java.time.Duration
import scala.concurrent.duration.FiniteDuration

/** An endomorphic stage that passes values through as long as a wall-clock deadline has not elapsed.
  *
  * The deadline is measured from the moment this instance is created: the timestamp `ts` is captured once at
  * construction using `now()`. On each invocation, if the elapsed time (`now() - ts`) is less than `duration` (in
  * nanoseconds), the stage yields `h8io.stages.Status.Success`; otherwise it yields `h8io.stages.Status.Complete`.
  *
  * The deadline is ''global'' in the sense that it is fixed at construction and never resets, unlike
  * [[h8io.stages.operators.LocalSoftDeadline]] which resets after each successful evolution transition.
  *
  * @param now
  *   a supplier of the current time in nanoseconds (typically `System.nanoTime _`)
  * @param duration
  *   the maximum allowed elapsed time in nanoseconds
  * @tparam T
  *   the value type passed through unchanged
  */
final class GlobalSoftDeadline[T] private (val now: () => Long, val duration: Long)
    extends Fruitful.Endo[T, Nothing] with BaseEvolution.Endo[T, Nothing] {
  private val ts: Long = now()

  override def apply(in: T): Yield.Some[T, T, Nothing] =
    Yield.Some(in, if (now() - ts < duration) Status.Success else Status.Complete, this)
}

/** Factory for [[GlobalSoftDeadline]] stages. */
object GlobalSoftDeadline {

  /** Creates a [[GlobalSoftDeadline]] with the given clock and duration in nanoseconds.
    *
    * @param now
    *   the clock supplier
    * @param duration
    *   the time budget in nanoseconds
    * @tparam T
    *   the value type
    */
  def apply[T](now: () => Long, duration: Long): GlobalSoftDeadline[T] = new GlobalSoftDeadline[T](now, duration)

  /** Extracts the clock and duration from a [[GlobalSoftDeadline]] instance. */
  def unapply[T](gsd: GlobalSoftDeadline[T]): Some[(() => Long, Long)] = Some((gsd.now, gsd.duration))

  /** Creates a [[GlobalSoftDeadline]] with a `scala.concurrent.duration.FiniteDuration` and `System.nanoTime` as the
    * clock.
    *
    * @param duration
    *   the time budget
    * @tparam T
    *   the value type
    */
  def apply[T](duration: FiniteDuration): Fruitful.Endo[T, Nothing] =
    GlobalSoftDeadline(System.nanoTime _, duration.toNanos)

  /** Creates a [[GlobalSoftDeadline]] with a `java.time.Duration` and `System.nanoTime` as the clock.
    *
    * @param duration
    *   the time budget
    * @tparam T
    *   the value type
    */
  def apply[T](duration: Duration): Fruitful.Endo[T, Nothing] = GlobalSoftDeadline(System.nanoTime _, duration.toNanos)
}
