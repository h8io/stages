package h8io.stages

trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {
  def alterand: S
}
