package h8io.stages.base

import h8io.stages.{OnDone, Stage}

trait BaseAlterator[_I, _O, _E, -I, +O, +E] extends Alterator[_I, _O, _E, I, O, E] {
  override type SkipContext = Unit

  override protected def beforeSkip(): SkipContext = {}
  override protected def afterSkip(context: SkipContext, onDone: OnDone[_I, _O, _E]): OnDone[I, O, E] =
    onDone map wrapAlterand

  protected def wrapAlterand(stage: Stage[_I, _O, _E]): Stage[I, O, E]

  override type DisposeContext = Unit

  override protected def beforeDispose(): DisposeContext = {}
  override protected def afterDispose(context: DisposeContext): Unit = {}
}
