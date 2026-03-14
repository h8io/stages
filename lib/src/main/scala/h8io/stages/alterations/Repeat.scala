package h8io.stages.alterations

import h8io.stages.*
import h8io.stages.base.{BaseDecorator, StageWithEvolution}

import scala.annotation.tailrec

final case class Repeat[-I, +O, +E](alterand: Stage[I, O, E])
    extends BaseDecorator[I, O, E] with StageWithEvolution[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = {
    @tailrec def repeat(stage: Stage[I, O, E]): Yield[I, O, E] = {
      val yld = stage(in)
      yld.status match {
        case Status.Success => repeat(yld.onDone.onSuccess())
        case Status.Complete =>
          yld.mapOnDone(Status.Success, onDone => Repeat(onDone.onComplete()))
        case error: Status.Error[E] => yld.mapOnDone(error, onDone => Repeat(onDone.onError()))
      }
    }
    repeat(alterand)
  }
}
