package h8io.stages.alterations

import h8io.stages.*
import h8io.stages.base.{BaseDecorator, OnDoneForStage}

import scala.annotation.tailrec

final case class Loop[T, E](alterand: Stage.Endo[T, E]) extends BaseDecorator[T, T, E] with OnDoneForStage.Endo[T, E] {
  override def apply(in: T): Yield[T, T, E] = {
    @tailrec def loop(stage: Stage[T, T, E], in: T): Yield[T, T, E] = {
      val yld = stage(in)
      yld.signal match {
        case Signal.Success =>
          yld match {
            case Yield.Some(out, _, _) => loop(yld.onDone.onSuccess(), out)
            case Yield.None(_, _) => Yield.None(Signal.Success, Loop(yld.onDone.onComplete()))
          }
        case Signal.Complete => yld.mapOnDone(Signal.Success, onDone => Loop(onDone.onComplete()))
        case error: Signal.Error[E] => yld.mapOnDone(error, onDone => Loop(onDone.onError()))
      }
    }
    loop(alterand, in)
  }

  override protected def wrapAlterand(stage: Stage[T, T, E]): Stage[T, T, E] = Loop(stage)
}
