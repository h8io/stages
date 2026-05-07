package h8io.stages.base

import h8io.stages.{Evolution, Stage, Status}

trait Stagnation[-I, +O, +E] extends Evolution[I, O, E] {
  self: Stage[I, O, E] =>
  override final def evolve(status: Status[?]): Stage[I, O, E] = this
  override def dispose(): Unit = ()
}
