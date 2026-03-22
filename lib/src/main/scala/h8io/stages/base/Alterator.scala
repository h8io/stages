package h8io.stages.base

import h8io.stages.Stage

trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {
  def alterand: S

  override final def dispose(): Unit = alterand.dispose()
}
