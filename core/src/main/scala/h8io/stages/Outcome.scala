package h8io.stages

/** Materialized result of a stage evaluation with a dispose callback.
  *
  * @tparam O
  *   output type (present only in [[Outcome.Some]])
  * @tparam E
  *   error/signal payload type
  */
trait Outcome[+O, +E] {

  /** Final signal for the evaluation. */
  val signal: Signal[E]

  /** Cleanup action for resources captured during evaluation. */
  val dispose: () => Unit
}

object Outcome {

  /** Outcome with an output value. */
  final case class Some[+O, +E](out: O, signal: Signal[E], dispose: () => Unit) extends Outcome[O, E]

  /** Outcome without an output value. */
  final case class None[+E](signal: Signal[E], dispose: () => Unit) extends Outcome[Nothing, E]
}
