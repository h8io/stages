package h8io.stages

trait Outcome[+O, +E] {
  val signal: Signal[E]
}

object Outcome {
  final case class Some[+O, +E](out: O, signal: Signal[E]) extends Outcome[O, E]

  final case class None[+E](signal: Signal[E]) extends Outcome[Nothing, E]
}
