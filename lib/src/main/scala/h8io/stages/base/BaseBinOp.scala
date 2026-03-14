package h8io.stages.base

trait BaseBinOp[-I, +LO, +RO, +O, +E] extends BinOp[I, LO, RO, O, E] {
  override type DisposeContext = Unit

  override def beforeDispose(): DisposeContext = ()
  override def afterDispose(ctx: DisposeContext): Unit = ()
}
