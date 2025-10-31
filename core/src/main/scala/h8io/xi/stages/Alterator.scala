package h8io.xi.stages

trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {
  val alterand: S

  override def dispose(): Unit = alterand.dispose()
}
