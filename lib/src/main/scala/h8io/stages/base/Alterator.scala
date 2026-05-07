package h8io.stages.base

import h8io.stages.Stage

/** A `h8io.stages.Stage` that wraps another stage (the ''alterand'') and forwards resource disposal to it.
  *
  * `Alterator` is the base trait for all unary operators and decorators that modify the behavior of a single inner
  * stage. It seals `h8io.stages.Stage.dispose` so that the wrapped stage's resources are always released when the
  * operator is disposed — concrete subclasses do not need to handle disposal themselves.
  *
  * '''Do not mix `Alterator` with traits that introduce independent state or their own `apply`/evolution logic''' (e.g.
  * [[SAMStage]] or similar mixins). `Alterator` assumes that [[alterand]] owns all resources: `dispose` is delegated to
  * it and it alone. A co-mixed trait that adds its own resources or overrides evolution without coordinating with
  * [[alterand]] breaks this contract silently — its resources will not be released and its evolution transitions will
  * be ignored.
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
