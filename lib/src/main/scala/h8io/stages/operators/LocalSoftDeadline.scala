package h8io.stages.operators

import h8io.stages
import h8io.stages.*
import h8io.stages.base.{BaseDecorator, Decoration}
import h8io.stages.std.DeadEnd

import scala.concurrent.duration.FiniteDuration

/** A decorator that stops the pipeline after a given duration has elapsed since the last successful
  * `h8io.stages.Evolution` transition.
  *
  * Unlike [[h8io.stages.std.GlobalSoftDeadline]], which measures time from the moment the stage is created,
  * `LocalSoftDeadline` resets its clock on every `onSuccess` transition: the timestamp is captured fresh each time the
  * stage is selected by `h8io.stages.Evolution.onSuccess`. On `onComplete` and `onError` transitions the clock is also
  * reset (to `now`), but the deadline is checked on the very next `apply` call.
  *
  * If the deadline is exceeded, the `h8io.stages.Status` of the current `h8io.stages.Yield` is upgraded to its break
  * variant (e.g. `Success` → `Complete`) by applying `break` to the status.
  *
  * If `duration ≤ 0` the factory methods return [[h8io.stages.std.DeadEnd]] directly, ensuring the pipeline immediately
  * terminates.
  *
  * @param tsSupplier
  *   a thunk that returns the timestamp captured at the last evolution transition
  * @param now
  *   a supplier of the current time in nanoseconds
  * @param duration
  *   the time budget in nanoseconds
  * @param alterand
  *   the inner stage whose deadline is enforced
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class LocalSoftDeadline[-I, +O, +E](
    tsSupplier: () => Long, now: () => Long, duration: Long, alterand: Stage[I, O, E])
    extends BaseDecorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = {
    val ts = tsSupplier()
    val yld = alterand(in)
    if (now() - ts >= duration) yld.map(identity, _.break, _.map(LocalSoftDeadline(now, now, duration, _)))
    else yld.map(identity, identity, LocalSoftDeadline.Evolution(() => ts, now, duration, _))
  }
}

/** Companion object and factory for [[LocalSoftDeadline]]. */
object LocalSoftDeadline {
  private[operators] final case class Evolution[-I, +O, +E](
      ts: () => Long, now: () => Long, duration: Long, evolution: stages.Evolution[I, O, E])
      extends stages.Evolution[I, O, E] {
    override def onSuccess(): Stage[I, O, E] = LocalSoftDeadline(ts, now, duration, evolution.onSuccess())
    override def onComplete(): Stage[I, O, E] = LocalSoftDeadline(now, now, duration, evolution.onComplete())
    override def onError(): Stage[I, O, E] = LocalSoftDeadline(now, now, duration, evolution.onError())
  }

  /** Wraps `stage` with a local deadline of the given `scala.concurrent.duration.FiniteDuration`.
    *
    * Returns [[h8io.stages.std.DeadEnd]] if `duration` is non-positive.
    */
  def apply[I, O, E](duration: FiniteDuration, stage: Stage[I, O, E]): Stage[I, O, E] = apply(duration.toNanos, stage)

  /** Wraps `stage` with a local deadline of the given `java.time.Duration`.
    *
    * Returns [[h8io.stages.std.DeadEnd]] if `duration` is non-positive.
    */
  def apply[I, O, E](duration: java.time.Duration, stage: Stage[I, O, E]): Stage[I, O, E] =
    apply(duration.toNanos, stage)

  @inline private def apply[I, O, E](duration: Long, stage: Stage[I, O, E]): Stage[I, O, E] =
    if (duration > 0) LocalSoftDeadline(now, now, duration, stage) else DeadEnd

  private val now: () => Long = System.nanoTime _

  /** Returns a [[h8io.stages.base.Decoration]] that applies this deadline to any stage.
    *
    * Useful when the same timeout should be applied to multiple stages.
    */
  def apply[I, O, E](duration: FiniteDuration): Decoration[I, O, E] = apply(duration, _)

  /** Returns a [[h8io.stages.base.Decoration]] that applies this deadline to any stage. */
  def apply[I, O, E](duration: java.time.Duration): Decoration[I, O, E] = apply(duration, _)
}
