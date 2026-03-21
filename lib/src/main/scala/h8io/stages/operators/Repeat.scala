package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.{BaseDecorator, BaseEvolution}

import scala.annotation.tailrec

final case class Repeat[-I, +O, +E](alterand: Stage[I, O, E])
    extends BaseDecorator[I, O, E] with BaseEvolution[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = {
    @tailrec def repeat(stage: Stage[I, O, E]): Yield[I, O, E] = {
      val yld = stage(in)
      yld.status match {
        case Status.Success => repeat(yld.evolution.onSuccess())
        case Status.Complete =>
          yld.map(identity, _ => Status.Success, evolution => Repeat(evolution.onComplete()))
        case _: Status.Error[E] => yld.map(identity, identity, evolution => Repeat(evolution.onError()))
      }
    }
    repeat(alterand)
  }
}
