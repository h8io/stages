package h8io.stages

/** The final result of executing a [[Stage]] via [[Stage.execute]].
  *
  * Unlike [[Yield]], an `Outcome` does not carry an [[Evolution]]; it is the terminal value returned to the caller
  * after the pipeline has finished processing a single input. The [[Status]] indicates whether execution succeeded
  * ([[Status.Success]]) or completed ([[Status.Complete]]), with or without accumulated errors. If disposing the
  * evolution fails, the exception is recorded in [[Outcome.disposeFailure]] without preventing the outcome from being
  * returned.
  *
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
sealed trait Outcome[+O, +E] {

  /** The status of the completed execution. */
  val status: Status[E]

  /** A non-fatal failure that occurred while disposing the evolution, if any. */
  val disposeFailure: Option[Throwable]
}

/** Companion object containing the two concrete variants of [[Outcome]]. */
object Outcome {

  /** An [[Outcome]] that carries an output value.
    *
    * Produced when the executed [[Stage]] yielded a [[Yield.Some]].
    *
    * @param out
    *   the output value produced by the stage
    * @param status
    *   the final execution status
    * @param disposeFailure
    *   a non-fatal exception raised while disposing the evolution, if any
    * @tparam O
    *   the output type (covariant)
    * @tparam E
    *   the error type (covariant)
    */
  final case class Some[+O, +E](out: O, status: Status[E], disposeFailure: Option[Throwable]) extends Outcome[O, E]

  /** An [[Outcome]] that carries no output value.
    *
    * Produced when the executed [[Stage]] yielded a [[Yield.None]], meaning the stage decided not to emit a value for
    * the given input.
    *
    * @param status
    *   the final execution status
    * @param disposeFailure
    *   a non-fatal exception raised while disposing the evolution, if any
    * @tparam E
    *   the error type (covariant)
    */
  final case class None[+E](status: Status[E], disposeFailure: Option[Throwable]) extends Outcome[Nothing, E]
}
