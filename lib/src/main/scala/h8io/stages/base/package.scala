package h8io.stages

import h8io.stages

import scala.util.Try

/** Type aliases used throughout the `lib` module for describing stage transformations.
  *
  * {{{
  * // A decorator is a stage itself: it wraps a Stage[I, O, E] and is one:
  * val myDecorator: Decorator[String, Int, Nothing] = CompleteIfNone(stage)
  *
  * // A decoration is the function building it; an alteration is the same with the types free to change:
  * val myDecoration: Decoration[String, Int, Nothing] = CompleteIfNone(_)
  * val myAlteration: Alteration[Stage[String, Int, Nothing], Stage[String, Option[Int], Nothing]] = Lift(_)
  * }}}
  */
package object base {

  /** An [[Alterator]]-based unary operator: a stage that wraps a single sub-stage `S` and potentially changes its
    * input/output/error types.
    */
  type UnaryOperator[+S <: Stage[I, ?, ?], -I, +O, +E] = Alterator[S, I, O, E]

  /** A [[UnaryOperator]] that wraps a `Stage[I, O, E]` and preserves its type — the standard shape for decorators (e.g.
    * [[h8io.stages.operators.CompleteIfNone]], [[h8io.stages.operators.KeepLastOutput]]).
    */
  type Decorator[-I, +O, +E] = UnaryOperator[Stage[I, O, E], I, O, E]

  /** A function that transforms one stage type into another.
    *
    * @tparam IS
    *   the input stage type (contravariant)
    * @tparam OS
    *   the output stage type (covariant)
    */
  type Alteration[-IS <: Stage.Any, +OS <: Stage.Any] = IS => OS

  /** An [[Alteration]] that transforms a `Stage[I, O, E]` into another `Stage[I, O, E]` of the same type — the standard
    * function shape passed to `h8io.stages.Evolution.map`.
    */
  type Decoration[I, O, E] = Alteration[Stage[I, O, E], Stage[I, O, E]]

  /** Extension methods for `Stage`. */
  implicit final class StageOps[I, O, E](val stage: Stage[I, O, E]) extends AnyVal {

    /** Lifts this stage into a [[ConstEvolution]] that returns it for every status branch.
      *
      * Useful inside the `h8io.stages.cycles` operators ([[h8io.stages.cycles.Loop]], [[h8io.stages.cycles.Repeat]],
      * [[h8io.stages.cycles.Reduce]] and [[h8io.stages.cycles.Fold]]) to produce the evolution embedded in a `Yield`.
      */
    @inline def toEvolution: Evolution[I, O, E] = ConstEvolution(stage)

    /** Lifts this stage into a [[ConstEvolution]] that returns it for every status branch and runs `dispose` when
      * disposed.
      *
      * The overload to reach for whenever the lifted stage is not the owner of what has to be released — an operator
      * keeping the inner evolution as the cleanup handle of the generation it has just built, or a stage recovering
      * from an exception with resources still to free.
      *
      * @param dispose
      *   the thunk `h8io.stages.Evolution.dispose` delegates to
      */
    @inline def toEvolution(dispose: () => Unit): ConstEvolution[I, O, E] = ConstEvolution(stage, dispose)

    /** Executes this stage end-to-end and returns a plain [[Outcome]].
      *
      * This is the reference terminal driver of the pipeline model. Internally this method:
      *   1. Applies the stage to `in`, obtaining a `h8io.stages.Yield`.
      *   1. Disposes the `h8io.stages.Evolution` carried by the `Yield` — since `execute` is a terminal operation, the
      *      continuation is not needed and the resources held by the stage must be released immediately.
      *   1. Wraps the result in an [[Outcome.Some]] or [[Outcome.None]].
      *
      * Disposal failures do not prevent the result from being returned. Any non-fatal exception raised by
      * `h8io.stages.Evolution.dispose` is captured in [[Outcome.disposeFailure]] and the outcome is still produced.
      * Fatal exceptions are not caught and will propagate.
      *
      * @param in
      *   the input value
      * @return
      *   [[Outcome.Some]] if this stage produced an output, [[Outcome.None]] otherwise
      */
    def execute(in: I): Outcome[O, E] = {
      val yld = stage(in)
      val disposeFailure = Try(yld.evolution.dispose()).failed.toOption
      yld match {
        case Yield.Some(out, status, _) => Outcome.Some(out, status, disposeFailure)
        case Yield.None(status, _) => Outcome.None(status, disposeFailure)
      }
    }
  }

  /** A [[BinaryOperator]] over two plain stages sharing the input and error types — the shape every binary operator in
    * the library has ([[h8io.stages.operators.And]], [[h8io.stages.operators.Or]], [[h8io.stages.operators.IAnd]],
    * `h8io.stages.cats.IOr`).
    *
    * @tparam I
    *   the shared input type (contravariant)
    * @tparam LO
    *   the left output type (covariant)
    * @tparam RO
    *   the right output type (covariant)
    * @tparam O
    *   the combined output type (covariant)
    * @tparam E
    *   the shared error type (covariant)
    */
  type BaseBinaryOperator[-I, +LO, +RO, +O, +E] = BinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, O, E]

  /** Companion object for [[BaseBinaryOperator]]. */
  object BaseBinaryOperator {

    /** The continuation of a [[BaseBinaryOperator]]: a pair of sub-evolutions that evolve and dispose together.
      *
      * Concrete operators implement `apply` alone — the reassembly of the two continuations into the operator's own
      * shape — and inherit the two halves of the shared contract:
      *
      *   - `evolve` selects both continuations on the same status, the right one first, and hands them to `apply`;
      *   - `dispose` releases both through `h8io.stages.Evolution.dispose`, right first, so that a failure of one still
      *     leaves the other released and is reported as suppressed.
      *
      * @tparam I
      *   the shared input type (contravariant)
      * @tparam LO
      *   the left output type (invariant: consumed and produced by the sub-evolution)
      * @tparam RO
      *   the right output type (invariant, likewise)
      * @tparam O
      *   the combined output type (covariant)
      * @tparam E
      *   the shared error type (covariant)
      */
    trait Evolution[-I, LO, RO, +O, +E] extends stages.Evolution[I, O, E] {

      /** The continuation of the left sub-stage. */
      def left: stages.Evolution[I, LO, E]

      /** The continuation of the right sub-stage. */
      def right: stages.Evolution[I, RO, E]

      /** Rebuilds the operator from the two continuations — normally the operator's own constructor.
        *
        * @param leftStage
        *   the stage the left sub-evolution selected
        * @param rightStage
        *   the stage the right sub-evolution selected
        */
      protected def apply[_I <: I, _E >: E](leftStage: Stage[_I, LO, _E], rightStage: Stage[_I, RO, _E])
          : Stage[_I, O, _E]

      override def evolve(status: Status[?]): Stage[I, O, E] = {
        val rightStage = right.evolve(status)
        val leftStage = left.evolve(status)
        apply(leftStage, rightStage)
      }

      override def dispose(): Unit = stages.Evolution.dispose(right, left)
    }
  }
}
