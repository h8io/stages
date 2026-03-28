package h8io.stages.base

import h8io.stages.{Stage, Yield}

import scala.util.control.NonFatal

/** A `h8io.stages.Stage` that separates normal execution from exception recovery.
  *
  * `SafeStage` seals `apply` and dispatches to [[body]] for the happy path. If `body` throws a non-fatal exception
  * (anything matched by `scala.util.control.NonFatal`), [[recover]] is called instead. Fatal exceptions (e.g.,
  * `OutOfMemoryError`) are not caught and propagate normally.
  *
  * Concrete classes implement [[body]] for the normal case and [[recover]] to convert exceptions into an appropriate
  * `h8io.stages.Yield` (typically a `h8io.stages.Yield.None` with an error status).
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait SafeStage[-I, +O, +E] extends Stage[I, O, E] {

  override final def apply(in: I): Yield[I, O, E] =
    try body(in)
    catch {
      case NonFatal(e) => recover(in, e)
    }

  /** The main execution logic invoked on each input.
    *
    * @param in
    *   the input value
    * @return
    *   a `h8io.stages.Yield` representing the result
    */
  def body(in: I): Yield[I, O, E]

  /** Called when [[body]] throws a non-fatal exception.
    *
    * @param in
    *   the input value that was being processed when the exception occurred
    * @param e
    *   the non-fatal exception thrown by [[body]]
    * @return
    *   a `h8io.stages.Yield` representing the error outcome
    */
  def recover(in: I, e: Throwable): Yield[I, O, E]
}
