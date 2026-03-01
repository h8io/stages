package h8io.stages

trait Alterator[_I, _O, _E, -I, +O, +E] extends Stage[I, O, E] {
  val alterand: Stage[_I, _O, _E]

  override def dispose(): Unit = alterand.dispose()
}
