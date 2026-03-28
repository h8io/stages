package h8io.stages.base

import h8io.stages.Stage

import scala.util.control.NonFatal

/** A `h8io.stages.Stage` that composes two sub-stages operating on the same input type.
  *
  * `BinaryOperator` provides exception-safe disposal of both sub-stages: `right` is disposed first, then `left`. If
  * `right.dispose()` throws a non-fatal exception, disposal of `left` is still attempted; any secondary exception from
  * `left` is added to the primary one as a suppressed exception before rethrowing.
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

  /** Disposes both sub-stages, `right` first.
    *
    * If `right.dispose()` throws, `left.dispose()` is still called and any exception it raises is attached as a
    * suppressed exception before rethrowing the primary one.
    */
  override final def dispose(): Unit = {
    try right.dispose()
    catch {
      case NonFatal(primary) =>
        try left.dispose()
        catch {
          case NonFatal(secondary) => primary.addSuppressed(secondary)
        } finally throw primary
    }
    left.dispose()
  }
}
