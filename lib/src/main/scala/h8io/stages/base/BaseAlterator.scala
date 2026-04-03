package h8io.stages.base

import h8io.stages.Stage

/** An [[Alterator]] with `Unit` as [[DisposeContext]] and no-op [[preDispose]] and [[postDispose]] hooks.
  *
  * Suitable for alterators that either need no pre- or post-disposal logic at all, or whose hooks do not need to pass
  * context between them. Both methods can still be overridden independently.
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
trait BaseAlterator[+S <: Stage.Any, -I, +O, +E] extends Alterator[S, I, O, E] {
  override type DisposeContext = Unit

  override def preDispose(): Unit = ()

  override def postDispose(ctx: DisposeContext): Unit = ()
}
