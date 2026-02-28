package h8io.stages

/** A stage that wraps another stage and can alter its behavior.
  *
  * Typical implementations delegate evaluation to `alterand` and customize signaling, continuation, or composition
  * behavior.
  *
  * @tparam S
  *   underlying stage type being altered
  * @tparam I
  *   input type
  * @tparam O
  *   output type
  * @tparam E
  *   error/signal payload type
  */
trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {

  /** Underlying stage being altered. */
  val alterand: S

  /** Disposes the underlying stage by default. */
  override def dispose(): Unit = alterand.dispose()
}
