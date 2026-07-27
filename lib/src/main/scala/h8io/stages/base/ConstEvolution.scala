package h8io.stages.base

import h8io.stages.{Evolution, Stage, Status}

/** An `Evolution` that returns the same constant `Stage` regardless of the status.
  *
  * Used by `StageOps.toEvolution` to lift a stateless stage into an evolution.
  *
  * @param stage
  *   the stage returned by every branch
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
final case class ConstEvolution[-I, +O, +E](stage: Stage[I, O, E], _dispose: () => Unit) extends Evolution[I, O, E] {
  override def evolve(status: Status[?]): Stage[I, O, E] = stage
  override def dispose(): Unit = _dispose()
}

object ConstEvolution {
  def apply[I, O, E](stage: Stage[I, O, E]): ConstEvolution[I, O, E] = new ConstEvolution(stage, () => ())

  /** The fruitful counterpart of [[ConstEvolution]]: the constant stage is a `h8io.stages.Stage.Fruitful`, so every
    * generation it returns is fruitful too.
    *
    * @param stage
    *   the fruitful stage returned by every branch
    * @param _dispose
    *   the cleanup action, as in [[ConstEvolution]]
    */
  final case class Fruitful[-I, +O, +E](stage: Stage.Fruitful[I, O, E], _dispose: () => Unit)
      extends Evolution.Fruitful[I, O, E] {
    override def evolve(status: Status[?]): Stage.Fruitful[I, O, E] = stage
    override def dispose(): Unit = _dispose()
  }

  object Fruitful {
    def apply[I, O, E](stage: Stage.Fruitful[I, O, E]): Fruitful[I, O, E] = new Fruitful(stage, () => ())
  }
}
