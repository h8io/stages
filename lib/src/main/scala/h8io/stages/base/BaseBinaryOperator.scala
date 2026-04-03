package h8io.stages.base

import h8io.stages.Stage

/** A [[BinaryOperator]] with `Unit` as [[BinaryOperator.DisposeContext]] and no-op [[BinaryOperator.preDispose]] and
  * [[BinaryOperator.postDispose]] hooks.
  *
  * Suitable for operators that either need no pre- or post-disposal logic at all, or whose hooks do not need to pass
  * context between them. Both methods can still be overridden independently.
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
trait BaseBinaryOperator[+LS <: Stage[I, ?, ?], +RS <: Stage[I, ?, ?], -I, +O, +E]
    extends BinaryOperator[LS, RS, I, O, E] {
  override type DisposeContext = Unit

  override def preDispose(): DisposeContext = ()

  override def postDispose(ctx: DisposeContext): Unit = ()
}
