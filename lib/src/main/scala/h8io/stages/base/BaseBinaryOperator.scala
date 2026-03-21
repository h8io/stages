package h8io.stages.base

trait BaseBinaryOperator[-I, +LO, +RO, +O, +E] extends BinaryOperator[I, LO, RO, O, E] {
  override type DisposeContext = Unit

  override def beforeDispose(): DisposeContext = ()
  override def afterDispose(ctx: DisposeContext): Unit = ()
}
