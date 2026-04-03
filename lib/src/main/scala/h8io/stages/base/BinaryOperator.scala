package h8io.stages.base

import h8io.stages.Stage

import scala.util.control.NonFatal

/** A `h8io.stages.Stage` that composes two sub-stages operating on the same input type.
  *
  * `BinaryOperator` provides exception-safe disposal of both sub-stages via a lifecycle hook sequence: [[preDispose]],
  * then `right.dispose()`, then `left.dispose()`, then [[postDispose]]. If any step throws, subsequent steps are still
  * attempted and any secondary exceptions are attached as suppressed exceptions to the primary one.
  *
  * Concrete subclasses (e.g. [[h8io.stages.operators.And]], [[h8io.stages.operators.Or]],
  * [[h8io.stages.operators.IAnd]]) implement `apply` to define how the two sub-stages are combined.
  *
  * @tparam LS
  *   the type of the left sub-stage (covariant, must accept input `I`)
  * @tparam RS
  *   the type of the right sub-stage (covariant, must accept input `I`)
  * @tparam I
  *   the shared input type (contravariant)
  * @tparam O
  *   the combined output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait BinaryOperator[+LS <: Stage[I, ?, ?], +RS <: Stage[I, ?, ?], -I, +O, +E] extends Stage[I, O, E] {

  /** The left sub-stage. */
  def left: LS

  /** The right sub-stage. */
  def right: RS

  /** The type of the value produced by [[preDispose]] and consumed by [[postDispose]]. */
  type DisposeContext

  /** Disposes both sub-stages, `right` first, then `left`.
    *
    * The disposal sequence is:
    *   1. [[preDispose]] — performs pre-disposal cleanup and returns a [[DisposeContext]].
    *   1. `right.dispose()` — releases the right sub-stage's resources.
    *   1. `left.dispose()` — releases the left sub-stage's resources.
    *   1. [[postDispose]] — performs post-disposal cleanup using the context from step 1.
    *
    * Exception semantics:
    *   - If [[preDispose]] throws, `right.dispose()` and `left.dispose()` are still called (in order), but
    *     [[postDispose]] is '''not''' invoked. Any exceptions from the dispose calls are suppressed.
    *   - If `right.dispose()` throws, `left.dispose()` and [[postDispose]] are still called; any exceptions they
    *     throw are suppressed.
    *   - If `left.dispose()` throws, [[postDispose]] is still called; any exception it throws is suppressed.
    *   - If [[postDispose]] throws on the normal (no-error) path, the exception propagates as-is.
    */
  override final def dispose(): Unit = {
    val ctx =
      try preDispose()
      catch {
        case NonFatal(primary) =>
          suppress(primary, right.dispose)
          suppress(primary, left.dispose)
          throw primary
      }
    try right.dispose()
    catch {
      case NonFatal(primary) =>
        suppress(primary, left.dispose)
        suppress(primary, () => postDispose(ctx))
        throw primary
    }
    try left.dispose()
    catch {
      case NonFatal(primary) =>
        suppress(primary, () => postDispose(ctx))
        throw primary
    }
    postDispose(ctx)
  }

  private def suppress(primary: Throwable, next: () => Unit): Unit =
    try next()
    catch {
      case NonFatal(secondary) => primary.addSuppressed(secondary)
    }

  /** Called at the start of [[dispose]], before `right.dispose()`.
    *
    * May perform pre-disposal cleanup and/or produce a [[DisposeContext]] that is passed to [[postDispose]]. If this
    * method throws, [[postDispose]] is '''not''' called — both sub-stages are still disposed with exceptions suppressed.
    */
  def preDispose(): DisposeContext

  /** Called at the end of [[dispose]], after both sub-stages have been disposed (normally or with an exception).
    *
    * Receives the [[DisposeContext]] returned by [[preDispose]]. Not called if [[preDispose]] threw.
    */
  def postDispose(ctx: DisposeContext): Unit
}
