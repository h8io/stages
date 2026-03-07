package h8io.stages.base

import h8io.stages.Stage

trait BinOp[-I, +LO, +RO, +O, +E] extends Stage[I, O, E] {
  val left: Stage[I, LO, E]
  val right: Stage[I, RO, E]
}
