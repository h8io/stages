package h8io.stages

import scala.util.Try

/** A single processing unit in a pipeline that transforms input values into [[Yield]] results.
  *
  * A `Stage` produces a [[Yield]] that carries an optional output of type `O`, a [[Status]], and an [[Evolution]]
  * strategy describing how to continue processing.
  *
  * Stages are contravariant in `I` and covariant in both `O` and `E`, which allows them to be composed safely in a
  * pipeline via the [[~>]] operator.
  *
  * ==Lifecycle==
  *
  * Every `Stage` instance participates in exactly one of these lifecycle paths during a pipeline run:
  *
  *   - '''Active''': [[apply]] is called with an input value. The stage processes it and returns a [[Yield]].
  *   - '''Skipped''': [[skip]] is called when the stage is bypassed — for example, because an upstream stage produced
  *     no output, or because of a non-inclusive binary operation. The stage must return its [[Evolution]] without
  *     performing any processing.
  *
  * Exactly one of [[apply]] or [[skip]] is called per pipeline run. Resource cleanup is the responsibility of the
  * [[Evolution]] returned from either call: [[Evolution.dispose]] releases the resources held by this stage, and after
  * that call the stage must be considered unusable. Fatal exceptions are not accounted for by this contract.
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
trait Stage[-I, +O, +E] extends (I => Yield[I, O, E]) {

  /** Applies this stage to the given input, producing a [[Yield]].
    *
    * @param in
    *   the input value
    * @return
    *   a [[Yield]] containing the optional output, status, and evolution
    */
  def apply(in: I): Yield[I, O, E]

  /** Returns the [[Evolution]] for this stage without processing any input.
    *
    * Any stage that participates in a pipeline run but does not process the current input must call `skip()` instead of
    * `apply`. Common triggers: an upstream stage produced no output ([[Yield.None]]), or a non-inclusive binary
    * operation excluded this branch. The stage must return its [[Evolution]] as it would have appeared had it run,
    * without consuming any input. Like `apply`, `skip()` may perform side effects — e.g. advancing internal state or
    * releasing resources that a decorator owns on behalf of an inner stage.
    *
    * See the ''Lifecycle'' section in [[Stage]] for the full contract.
    *
    * @return
    *   the [[Evolution]] representing how the pipeline should continue from this stage
    */
  def skip(): Evolution[I, O, E]

  /** Executes this stage end-to-end and returns a plain [[Outcome]].
    *
    * Internally this method:
    *   1. Applies the stage to `in`, obtaining a [[Yield]].
    *   1. Disposes the [[Evolution]] carried by the [[Yield]] — since `execute` is a terminal operation, the
    *      continuation is not needed and the resources held by this stage must be released immediately.
    *   1. Wraps the result in an [[Outcome.Some]] or [[Outcome.None]].
    *
    * Disposal failures do not prevent the result from being returned. Any non-fatal exception raised by
    * [[Evolution.dispose]] is captured in [[Outcome.disposeFailure]] and the outcome is still produced. Fatal
    * exceptions are not caught and will propagate.
    *
    * @param in
    *   the input value
    * @return
    *   [[Outcome.Some]] if this stage produced an output, [[Outcome.None]] otherwise
    */
  @inline final def execute(in: I): Outcome[O, E] = {
    val yld = this(in)
    val disposeFailure = Try(yld.evolution.dispose()).failed.toOption
    yld match {
      case Yield.Some(out, status, _) => Outcome.Some(out, status, disposeFailure)
      case Yield.None(status, _) => Outcome.None(status, disposeFailure)
    }
  }

  /** Composes this stage with `that`, producing a new stage that feeds the output of this stage into `that`.
    *
    * The resulting [[Stage.AndThen]] feeds the output of this stage into `that`. If this stage produces an output, the
    * statuses and evolutions of both stages are merged; if it produces no output, only the evolutions are composed and
    * `that` is not invoked for the current input.
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
    *   - If `upstream` produces a [[Yield.Some]], the output is passed to `downstream`.
    *   - If `upstream` produces a [[Yield.None]], the evolution is composed with `downstream` so that it is applied
    *     when the pipeline resumes.
    *
    * @param upstream
    *   the first stage in the sequence
    * @param downstream
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
  final case class AndThen[-I, OI, +O, +E](upstream: Stage[I, OI, E], downstream: Stage[OI, O, E])
      extends Stage[I, O, E] {
    override def apply(in: I): Yield[I, O, E] =
      upstream(in) match {
        case some @ Yield.Some(out, _, _) => some.compose(downstream(out))
        case none: Yield.None[I, OI, E] => none.compose(downstream.skip())
      }

    override def skip(): Evolution[I, O, E] = upstream.skip().compose(downstream.skip())
  }
}
