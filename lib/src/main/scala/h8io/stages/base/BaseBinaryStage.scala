package h8io.stages.base

trait BaseBinaryStage[-I, +LO, +RO, +O, +E] extends BinaryStage[I, LO, RO, O, E] {
  override type DisposeContext = Unit

  override def beforeDispose(): DisposeContext = ()
  override def afterDispose(ctx: DisposeContext): Unit = ()
}
