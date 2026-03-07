package h8io.stages.base

import h8io.stages.{Alterator, OnDone, Stage}

trait BaseAlterator[_I, _O, _E, -I, +O, +E] extends Alterator[Stage[_I, _O, _E], I, O, E] {
  override val alterand: Stage[_I, _O, _E]

  protected def withAlterand(stage: Stage[_I, _O, _E]): Stage[I, O, E]

  override def skip: OnDone[I, O, E] = alterand.skip.map(withAlterand)

  override def dispose(): Unit = alterand.dispose()
}
