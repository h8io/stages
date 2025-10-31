package h8io.xi.stages.binops

import h8io.xi.stages.Stage

trait BinaryOp[-I, +LO, +RO, +O, +E] extends Stage[I, O, E] {
  val left: Stage[I, LO, E]
  val right: Stage[I, RO, E]

  override def dispose(): Unit = {
    right.dispose()
    left.dispose()
  }
}
