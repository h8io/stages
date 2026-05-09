package h8io.stages.base

import h8io.stages.Stage

/** A `h8io.stages.Stage` that wraps another stage (the ''alterand'').
  *
  * `Alterator` is the base trait for all unary operators and decorators that modify the behavior of a single inner
  * stage. Resource disposal is the responsibility of the `h8io.stages.Evolution` returned by `apply` or `skip` —
  * concrete subclasses must ensure their evolution delegates `dispose` to [[alterand]]'s evolution.
  *
  * '''Do not mix `Alterator` with traits that introduce independent state or their own `apply`/evolution logic''' (e.g.
  * [[SAMStage]] or similar mixins). `Alterator` assumes that [[alterand]] owns all resources: if a co-mixed trait
  * provides its own `skip` or evolution without coordinating with [[alterand]], those resources will not be released
  * and the evolution transitions will be ignored.
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
