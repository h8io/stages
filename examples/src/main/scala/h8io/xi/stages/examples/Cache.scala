package h8io.xi.stages.examples

import h8io.xi.stages.*
import h8io.xi.stages.std.Fruitful

final case class Cache[-I, +O, +E](alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
  def apply(in: I): Yield[I, O, E] =
    alterand(in) match {
      case Yield.Some(out, Signal.Success, onDone) =>
        Yield.Some(
          out,
          Signal.Success,
          new OnDone[I, O, E] {
            def onSuccess(): Stage[I, O, E] = Cache.Cached(out, onDone.onSuccess())
            def onComplete(): Stage[I, O, E] = Cache(onDone.onComplete())
            def onError(): Stage[I, O, E] = Cache(onDone.onError())
          }
        )
      case yld => yld.mapOnDone(_.map(Cache(_)))
    }
}

object Cache {
  private[examples] final case class Cached[-I, +O, +E](out: O, alterand: Stage[I, O, E])
      extends Decorator[I, O, E] with Fruitful[I, O, E] {
    def apply(in: I): Yield.Some[I, O, E] = Yield.Some(out, Signal.Success, this)

    override def onComplete(): Stage[I, O, E] = Cache(alterand)
    override def onError(): Stage[I, O, E] = Cache(alterand)
  }
}
