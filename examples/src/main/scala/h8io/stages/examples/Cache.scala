package h8io.stages.examples

import h8io.stages.*
import h8io.stages.base.{Decorator, Fruitful}

final case class Cache[-I, +O, +E](alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
  /*
   * Example: a simple cache decorator for stages.
   * On Success it switches to a Cached stage that replays the last output without
   * re-running the wrapped stage. On Complete or Error the cache is dropped and
   * the original stage is used again. This demonstrates how evolution can swap
   * behavior based on the previous status.
   */
  override def apply(in: I): Yield[I, O, E] =
    alterand(in) match {
      case Yield.Some(out, Status.Success, evolution) =>
        Yield.Some(out, Status.Success, Cache._Evolution(out, evolution))
      case yld => yld.map(identity, identity, _.map(Cache(_)))
    }

  override def skip(): Evolution[I, O, E] = alterand.skip().map(Cache(_))
}

object Cache {
  private[examples] final case class Cached[-I, +O, +E](out: O, alterand: Stage[I, O, E])
      extends Decorator[I, O, E] with Fruitful[I, O, E] {
    override def apply(in: I): Yield.Some[I, O, E] = Yield.Some(out, Status.Success, skip())
    override def skip(): Evolution[I, O, E] = new _Evolution[I, O, E](out, alterand.skip())
  }

  private final case class _Evolution[-I, +O, +E](out: O, evolution: Evolution[I, O, E]) extends Evolution[I, O, E] {
    override def evolve(status: Status[?]): Stage[I, O, E] =
      status match {
        case Status.Success => Cache.Cached(out, evolution.evolve(Status.Success))
        case complete: Status.Complete[?] => Cache(evolution.evolve(complete))
      }

    override def dispose(): Unit = evolution.dispose()
  }
}
