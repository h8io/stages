package h8io.stages.base

import h8io.stages.Stage

import scala.util.control.NonFatal

/** A `h8io.stages.Stage` that wraps another stage (the ''alterand'') and forwards resource disposal to it.
  *
  * `Alterator` is the base trait for all unary operators and decorators that modify the behavior of a single inner
  * stage. It seals `h8io.stages.Stage.dispose` so that the wrapped stage's resources are always released when the
  * operator is disposed — concrete subclasses do not need to handle disposal themselves.
  *
  * @tparam S
  *   the concrete type of the wrapped stage (covariant)
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {

  /** The wrapped stage whose behavior is modified by this operator. */
  def alterand: S

  /** The type of the value produced by [[preDispose]] and consumed by [[postDispose]]. */
  type DisposeContext

  /** Disposes this stage by forwarding the call to [[alterand]].
    *
    * The disposal sequence is:
    *   1. [[preDispose]] — performs pre-disposal cleanup and returns a [[DisposeContext]].
    *   1. `alterand.dispose()` — releases the wrapped stage's resources.
    *   1. [[postDispose]] — performs post-disposal cleanup using the context from step 1.
    *
    * Exception semantics:
    *   - If [[preDispose]] throws, `alterand.dispose()` is still called, but [[postDispose]] is '''not''' invoked.
    *   - If `alterand.dispose()` throws, [[postDispose]] is still called; any exception it throws is added as a
    *     suppressed exception to the one from `alterand.dispose()`.
    *   - If [[postDispose]] throws on the normal (no-error) path, the exception propagates as-is.
    */
  override final def dispose(): Unit = {
    val context: DisposeContext =
      try preDispose()
      catch {
        case NonFatal(primary) =>
          try alterand.dispose()
          catch {
            case NonFatal(secondary) => primary.addSuppressed(secondary)
          }
          throw primary
      }
    try alterand.dispose()
    catch {
      case NonFatal(primary) =>
        try postDispose(context)
        catch {
          case NonFatal(secondary) => primary.addSuppressed(secondary)
        }
        throw primary
    }
    postDispose(context)
  }

  /** Called at the start of [[dispose]], before `alterand.dispose()`.
    *
    * May perform pre-disposal cleanup and/or produce a [[DisposeContext]] that is passed to [[postDispose]]. If this
    * method throws, [[postDispose]] is '''not''' called — the exception propagates after `alterand.dispose()` is
    * attempted.
    */
  def preDispose(): DisposeContext

  /** Called at the end of [[dispose]], after `alterand.dispose()` completes (normally or with an exception).
    *
    * Receives the [[DisposeContext]] returned by [[preDispose]]. Not called if [[preDispose]] threw.
    */
  def postDispose(ctx: DisposeContext): Unit
}
