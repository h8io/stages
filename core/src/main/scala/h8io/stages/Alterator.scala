package h8io.stages

trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {
  val alterand: S

  override def dispose(): Unit = alterand.dispose()
}
