package h8io.xi.stages

trait Outcome[+O, +E] {
  val signal: Signal[E]
  val dispose: () => Unit
}

object Outcome {
  final case class Some[+O, +E](out: O, signal: Signal[E], dispose: () => Unit) extends Outcome[O, E]

  final case class None[+E](signal: Signal[E], dispose: () => Unit) extends Outcome[Nothing, E]
}
