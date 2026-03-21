package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.BaseDecorator
import h8io.stages.std.DeadEnd

import scala.concurrent.duration.FiniteDuration

final case class LocalSoftDeadline[-I, +O, +E](
    tsSupplier: () => Long, now: () => Long, duration: Long, alterand: Stage[I, O, E])
    extends BaseDecorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = {
    val ts = tsSupplier()
    val yld = alterand(in)
    if (now() - ts >= duration) yld.map(identity, _.break, _.map(LocalSoftDeadline(now, now, duration, _)))
    else yld.map(identity, identity, LocalSoftDeadline._Evolution(() => ts, now, duration, _))
  }
}

object LocalSoftDeadline {
  private[operators] final case class _Evolution[-I, +O, +E](
      ts: () => Long, now: () => Long, duration: Long, evolution: Evolution[I, O, E])
      extends Evolution[I, O, E] {
    override def onSuccess(): Stage[I, O, E] = LocalSoftDeadline(ts, now, duration, evolution.onSuccess())
    override def onComplete(): Stage[I, O, E] = LocalSoftDeadline(now, now, duration, evolution.onComplete())
    override def onError(): Stage[I, O, E] = LocalSoftDeadline(now, now, duration, evolution.onError())
  }

  def apply[I, O, E](duration: FiniteDuration, stage: Stage[I, O, E]): Stage[I, O, E] = apply(duration.toNanos, stage)

  def apply[I, O, E](duration: java.time.Duration, stage: Stage[I, O, E]): Stage[I, O, E] =
    apply(duration.toNanos, stage)

  @inline private def apply[I, O, E](duration: Long, stage: Stage[I, O, E]): Stage[I, O, E] =
    if (duration > 0) LocalSoftDeadline(now, now, duration, stage) else DeadEnd

  private val now: () => Long = System.nanoTime _

  def apply[I, O, E](duration: FiniteDuration): Decoration[I, O, E] = apply(duration, _)

  def apply[I, O, E](duration: java.time.Duration): Decoration[I, O, E] = apply(duration, _)
}
