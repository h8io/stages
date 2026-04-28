package h8io.stages

import h8io.stages

import scala.util.control.NonFatal

/** Type aliases used throughout the `lib` module for describing stage transformations.
  *
  * {{{
  * // A decorator wraps a Stage[I, O, E] and returns another Stage[I, O, E]:
  * val myDecorator: Decorator[String, Int, Nothing] = BreakIfNone(_)
  *
  * // An alteration converts any Stage to a (possibly different) Stage:
  * val myAlt: Alteration[Stage[String, Int, Nothing], Stage[String, Option[Int], Nothing]] = Lift(_)
  * }}}
  */
package object base {

  /** An [[Alterator]]-based unary operator: a stage that wraps a single sub-stage `S` and potentially changes its
    * input/output/error types.
    */
  type UnaryOperator[+S <: Stage[I, ?, ?], -I, +O, +E] = Alterator[S, I, O, E]

  /** A [[UnaryOperator]] that wraps a `Stage[I, O, E]` and preserves its type — the standard shape for decorators (e.g.
    * [[h8io.stages.operators.BreakIfNone]], [[h8io.stages.operators.KeepLastOutput]]).
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
      * Useful inside [[h8io.stages.operators.Loop]] and [[h8io.stages.operators.Repeat]] to produce the evolution
      * embedded in a `Yield` without mixing in [[BaseEvolution]].
      */
    @inline def toEvolution: Evolution[I, O, E] = ConstEvolution(stage)

    @inline def toEvolution(dispose: () => Unit): ConstEvolution[I, O, E] = ConstEvolution(stage, dispose)
  }

  type BaseBinaryOperator[-I, +LO, +RO, +O, +E] = BinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, O, E]

  object BaseBinaryOperator {
    trait Evolution[-I, LO, RO, +O, +E] extends stages.Evolution[I, O, E] {
      def left: stages.Evolution[I, LO, E]
      def right: stages.Evolution[I, RO, E]

      protected def apply[_I <: I, _E >: E](leftStage: Stage[_I, LO, _E], rightStage: Stage[_I, RO, _E])
          : Stage[_I, O, _E]

      override def apply(status: Status[?]): Stage[I, O, E] = {
        val rightStage = right(status)
        val leftStage = left(status)
        apply(leftStage, rightStage)
      }

      override def dispose(): Unit = {
        try right.dispose()
        catch {
          case NonFatal(primary) =>
            try left.dispose()
            catch { case NonFatal(secondary) => primary.addSuppressed(secondary) }
            finally throw primary
        }
        left.dispose()
      }
    }
  }
}
