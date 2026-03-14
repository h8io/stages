package h8io.stages.examples

import h8io.stages.*
import h8io.stages.base.{BaseDecorator, Fruitful, StageWithEvolution}

final case class Cache[-I, +O, +E](alterand: Stage[I, O, E])
    extends BaseDecorator[I, O, E] with StageWithEvolution[I, O, E] {
  override def apply(in: I): Yield[I, O, E] =
    alterand(in) match {
      case Yield.Some(out, Status.Success, evolution) =>
        Yield.Some(
          out,
          Status.Success,
          new Evolution[I, O, E] {
            override def onSuccess(): Stage[I, O, E] = Cache.Cached(out, evolution.onSuccess())
            override def onComplete(): Stage[I, O, E] = Cache(evolution.onComplete())
            override def onError(): Stage[I, O, E] = Cache(evolution.onError())
          }
        )
      case yld => yld.mapEvolution(_.map(Cache(_)))
    }
}

object Cache {
  private[examples] final case class Cached[-I, +O, +E](out: O, alterand: Stage[I, O, E])
      extends BaseDecorator[I, O, E] with Fruitful[I, O, E] with StageWithEvolution[I, O, E] {
    override def apply(in: I): Yield.Some[I, O, E] = Yield.Some(out, Status.Success, this)

    override def onComplete(): Stage[I, O, E] = Cache(alterand)
    override def onError(): Stage[I, O, E] = Cache(alterand)
  }
}
