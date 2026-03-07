package h8io.stages.std

import h8io.stages
import h8io.stages.Signal
import h8io.stages.base.BaseStage

sealed case class DeadEnd(_dispose: () => Unit) extends BaseStage[Any, Nothing, Nothing] {
  final val Yield: stages.Yield.None[Any, Nothing, Nothing] =
    stages.Yield.None[Any, Nothing, Nothing](Signal.Complete, this)

  override final def apply(in: Any): stages.Yield.None[Any, Nothing, Nothing] = Yield

  override final def dispose(): Unit = _dispose()
}

object DeadEnd extends DeadEnd({ () => })
