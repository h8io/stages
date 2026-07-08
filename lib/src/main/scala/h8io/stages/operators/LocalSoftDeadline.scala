package h8io.stages.operators

import h8io.stages
import h8io.stages.base.{Decoration, Decorator}
import h8io.stages.std.DeadEnd
import h8io.stages.{Stage, Status, Yield}

import scala.concurrent.duration.FiniteDuration

/** A decorator that stops the pipeline when a time budget measured from the beginning of the current unit of work is
  * exhausted.
  *
  * Unlike [[h8io.stages.std.GlobalSoftDeadline]], which measures time from the moment the stage is created and never
  * resets, `LocalSoftDeadline` measures time from the `apply` call that started the current unit of work: the timestamp
  * is captured at that call and preserved for as long as the evolution is invoked with `h8io.stages.Status.Success`.
  * When the evolution is invoked with any other status, the clock is reset — the new window starts at the next `apply`
  * call. The same reset happens after the deadline has been exceeded.
  *
  * The deadline is ''soft'': it is checked after the inner stage has already been applied. If it is exceeded, a
  * `h8io.stages.Status.Success` in the current `h8io.stages.Yield` is upgraded to an errorless
  * `h8io.stages.Status.Complete`; any other status is left unchanged.
  *
  * If `duration ≤ 0` the factory methods return [[h8io.stages.std.DeadEnd]] directly, ensuring the pipeline immediately
  * terminates.
  *
  * @param tsSupplier
  *   a thunk that returns the timestamp of the start of the current deadline window
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
    extends Decorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = {
    val ts = tsSupplier()
    val yld = alterand(in)
    if (now() - ts >= duration) yld.map(
      identity,
      {
        case Status.Success => Status.complete
        case other => other
      },
      LocalSoftDeadline.Evolution(now, now, duration, _))
    else yld.map(identity, identity, LocalSoftDeadline.Evolution(() => ts, now, duration, _))
  }

  override def skip(): stages.Evolution[I, O, E] =
    LocalSoftDeadline.Evolution(tsSupplier, now, duration, alterand.skip())
}

/** Companion object and factory for [[LocalSoftDeadline]]. */
object LocalSoftDeadline {

  private[operators] final case class Evolution[-I, +O, +E](
      tsSupplier: () => Long,
      now: () => Long,
      duration: Long,
      evolution: stages.Evolution[I, O, E])
      extends stages.Evolution[I, O, E] {
    override def evolve(status: Status[?]): Stage[I, O, E] =
      LocalSoftDeadline(
        status match {
          case Status.Success => tsSupplier
          case _ => now
        },
        now,
        duration,
        evolution.evolve(status))

    override def dispose(): Unit = evolution.dispose()
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
