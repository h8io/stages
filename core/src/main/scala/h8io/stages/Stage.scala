package h8io.stages

import scala.util.Try
import scala.util.control.NonFatal

/** A single processing unit in a pipeline that transforms input values into [[Yield]] results.
  *
  * A `Stage` is a functional interface: given an input of type `I`, it produces a [[Yield]] that carries an optional
  * output of type `O`, a [[Status]], and an [[Evolution]] strategy describing how to continue processing.
  *
  * Stages are contravariant in `I` and covariant in both `O` and `E`, which allows them to be composed safely in a
  * pipeline via the [[~>]] operator.
  *
  * Example — building a pipeline:
  * {{{
  * val parse: Stage[String, Int, String]  = ...
  * val double: Stage[Int, Int, String]    = ...
  * val pipeline: Stage[String, Int, String] = parse ~> double
  * }}}
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
@FunctionalInterface
trait Stage[-I, +O, +E] extends (I => Yield[I, O, E]) {

  /** Applies this stage to the given input, producing a [[Yield]].
    *
    * @param in
    *   the input value
    * @return
    *   a [[Yield]] containing the optional output, status, and evolution
    */
  def apply(in: I): Yield[I, O, E]

  /** Releases any resources held by this stage.
    *
    * Called by pipeline owners (for example, [[Stage.AndThen]]) when disposing a composed stage. [[execute]] disposes
    * only the next stage selected from the [[Evolution]] and does not call `dispose()` on this stage.
    *
    * The default implementation is a no-op; override it when the stage holds external resources (connections, file
    * handles, etc.).
    */
  def dispose(): Unit = {}

  /** Executes this stage end-to-end and returns a plain [[Outcome]].
    *
    * Internally this method:
    *   1. Applies the stage to `in`, obtaining a [[Yield]].
    *   1. Selects the next stage from the [[Evolution]] based on the current [[Status]].
    *   1. Attempts to dispose the selected next stage.
    *   1. Wraps the result in an [[Outcome.Some]] or [[Outcome.None]].
    *
    * Disposal failures do not prevent the result from being returned. Any non-fatal exception raised by disposing the
    * selected next stage is captured in [[Outcome.disposeFailure]] and the outcome is still produced. Fatal exceptions
    * are not caught and will propagate.
    *
    * @param in
    *   the input value
    * @return
    *   [[Outcome.Some]] if this stage produced an output, [[Outcome.None]] otherwise
    */
  @inline final def execute(in: I): Outcome[O, E] = {
    val yld = this(in)
    val disposeFailure = Try(yld.status(yld.evolution).dispose()).failed.toOption
    yld match {
      case Yield.Some(out, status, _) => Outcome.Some(out, status, disposeFailure)
      case Yield.None(status, _) => Outcome.None(status, disposeFailure)
    }
  }

  /** Composes this stage with `that`, producing a new stage that feeds the output of this stage into `that`.
    *
    * The resulting [[Stage.AndThen]] merges the statuses and evolutions of both stages.
    *
    * @param that
    *   the stage to execute after this one
    * @tparam _O
    *   the output type of the composed pipeline
    * @tparam _E
    *   the combined error type (must be a supertype of `E`)
    * @return
    *   a composed stage `this ~> that`
    */
  @inline final def ~>[_O, _E >: E](that: Stage[O, _O, _E]): Stage[I, _O, _E] = Stage.AndThen(this, that)

  /** Internal reverse-composition: equivalent to `that ~> this`.
    *
    * Used inside [[Evolution]] to compose continuations without exposing the operator publicly.
    */
  @inline private[stages] final def <~[_I, _E >: E](that: Stage[_I, I, _E]): Stage[_I, O, _E] = that ~> this
}

/** Companion object for [[Stage]], containing type aliases and the [[AndThen]] implementation. */
object Stage {

  /** A stage whose input and output types are the same — an endomorphism on `T`. */
  type Endo[T, +E] = Stage[T, T, E]

  /** An existential alias for a stage with unknown type parameters, useful as a common supertype. */
  type Any = Stage[?, ?, ?]

  /** A [[Stage]] composed of two sequential stages.
    *
    * When applied to an input:
    *   - If `previous` produces a [[Yield.Some]], the output is passed to `next`.
    *   - If `previous` produces a [[Yield.None]], the evolution is composed with `next` so that it is applied when the
    *     pipeline resumes.
    *
    * Disposal is performed in reverse order (`next` first, then `previous`). If `next.dispose()` throws, the exception
    * from `previous.dispose()` is added as a suppressed exception.
    *
    * @param previous
    *   the first stage in the sequence
    * @param next
    *   the second stage in the sequence
    * @tparam I
    *   input type of the pipeline
    * @tparam OI
    *   intermediate type between the two stages
    * @tparam O
    *   output type of the pipeline
    * @tparam E
    *   error type
    */
  final case class AndThen[-I, OI, +O, +E](previous: Stage[I, OI, E], next: Stage[OI, O, E]) extends Stage[I, O, E] {
    override def apply(in: I): Yield[I, O, E] =
      previous(in) match {
        case some @ Yield.Some(out, _, _) => some.compose(next(out))
        case none: Yield.None[I, OI, E] => none.compose(next)
      }

    override def dispose(): Unit = {
      try next.dispose()
      catch {
        case NonFatal(primary) => try previous.dispose()
          catch {
            case NonFatal(secondary) =>
              primary.addSuppressed(secondary)
          } finally throw primary
      }
      previous.dispose()
    }
  }
}
