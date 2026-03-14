package h8io.stages.std

import h8io.stages
import h8io.stages.base.BaseEvolution
import h8io.stages.{Stage, Status}

sealed case class DeadEnd(_dispose: () => Unit)
    extends Stage[Any, Nothing, Nothing] with BaseEvolution[Any, Nothing, Nothing] {
  final val Yield: stages.Yield.None[Any, Nothing, Nothing] =
    stages.Yield.None[Any, Nothing, Nothing](Status.Complete, this)

  override final def apply(in: Any): stages.Yield.None[Any, Nothing, Nothing] = Yield

  override final def dispose(): Unit = _dispose()
}

object DeadEnd extends DeadEnd({ () => })
