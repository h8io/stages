package h8io.stages.base

import h8io.stages.Stage

trait BaseAlterator[+S <: Stage.Any, -I, +O, +E] extends Alterator[S, I, O, E] {
  override type DisposeContext = Unit

  override def beforeDispose(): DisposeContext = ()
  override def afterDispose(ctx: DisposeContext): Unit = ()
}
