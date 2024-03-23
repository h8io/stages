package io.h8.stages.test
import io.h8.stages.{Behavior, State}

object DoneStage extends TestStage[Any, Nothing] {
  override def apply(v: Any): State[Any, Nothing] = State.Done(() => Behavior.Complete)
}
