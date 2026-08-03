package h8io.stages.base

import h8io.stages.Stage

/** A `h8io.stages.Stage` that wraps another stage (the ''alterand'').
  *
  * `Alterator` is the base trait for all unary operators and decorators that modify the behavior of a single inner
  * stage. Resource disposal is the responsibility of the `h8io.stages.Evolution` returned by `apply` or `skip` —
  * concrete subclasses must ensure their evolution delegates `dispose` to [[alterand]]'s evolution.
  *
  * '''Do not mix `Alterator` with traits that supply the stage's own continuation''' — its `skip`, `evolve` or
  * `dispose` (e.g. [[SAMStage]], [[Stagnation]]). An alterator's evolution has to be built from [[alterand]]'s: a mixin
  * that answers `this` instead — and both of those seal it that way — leaves [[alterand]] neither evolved nor disposed,
  * with nothing to signal it.
  *
  * Traits that only shape `apply` are safe to mix in, since they leave the continuation to the alterator itself:
  * [[SafeStage]] and [[Fruitful]] are both used that way here (see [[h8io.stages.operators.Safe]] and
  * [[h8io.stages.operators.Lift]]).
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
}
