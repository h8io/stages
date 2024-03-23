package io.h8.stages.test
import io.h8.stages.{Behavior, Stage, State}

object DoneStage extends Stage.Safe[Any, Nothing] {
  override def apply(v: Any): State[Any, Nothing] = State.Done(() => Behavior.Complete)
}
