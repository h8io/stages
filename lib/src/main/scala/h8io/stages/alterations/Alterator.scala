package h8io.stages.alterations

import h8io.stages.Stage

trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {
  val alterand: S

  override def dispose(): Unit = alterand.dispose()
}
