package h8io.xi.stages.std

import h8io.xi.stages
import h8io.xi.stages.{Signal, Stage}

sealed case class DeadEnd(_dispose: () => Unit) extends Stage[Any, Nothing, Nothing] {
  final val Yield: stages.Yield.None[Any, Nothing, Nothing] =
    stages.Yield.None[Any, Nothing, Nothing](Signal.Complete, this)

  final def apply(in: Any): stages.Yield.None[Any, Nothing, Nothing] = Yield

  override final def dispose(): Unit = _dispose()
}

object DeadEnd extends DeadEnd({ () => })
