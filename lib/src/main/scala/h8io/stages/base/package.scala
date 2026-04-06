package h8io.stages

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

  implicit final class StageOps[I, O, E](val stage: Stage[I, O, E]) extends AnyVal {
    def evolution: Evolution[I, O, E] = StageEvolution(stage)
  }
}
