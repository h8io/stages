package h8io.stages.base

import h8io.stages.Stage

trait BaseBinaryOperator[+LS <: Stage[I, ?, ?], +RS <: Stage[I, ?, ?], -I, +O, +E]
    extends BinaryOperator[LS, RS, I, O, E] {
  override type DisposeContext = Unit

  override def beforeDispose(): DisposeContext = ()
  override def afterDispose(ctx: DisposeContext): Unit = ()
}
