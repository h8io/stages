package h8io.stages

trait Outcome[+O, +E] {
  val status: Status[E]
}

object Outcome {
  final case class Some[+O, +E](out: O, status: Status[E]) extends Outcome[O, E]

  final case class None[+E](status: Status[E]) extends Outcome[Nothing, E]
}
