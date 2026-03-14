package h8io.stages.alterations

import h8io.stages.*
import h8io.stages.base.{BaseDecorator, BaseEvolution}

import scala.annotation.tailrec

final case class Loop[T, +E](alterand: Stage.Endo[T, E]) extends BaseDecorator[T, T, E] with BaseEvolution.Endo[T, E] {
  override def apply(in: T): Yield[T, T, E] = {
    @tailrec def loop(stage: Stage[T, T, E], in: T): Yield[T, T, E] = {
      val yld = stage(in)
      yld.status match {
        case Status.Success =>
          yld match {
            case Yield.Some(out, _, _) => loop(yld.evolution.onSuccess(), out)
            case Yield.None(_, _) => Yield.None(Status.Success, Loop(yld.evolution.onComplete()))
          }
        case Status.Complete => yld.mapEvolution(Status.Success, evolution => Loop(evolution.onComplete()))
        case error: Status.Error[E] => yld.mapEvolution(error, evolution => Loop(evolution.onError()))
      }
    }
    loop(alterand, in)
  }
}
