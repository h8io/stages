package io.h8.stages.test

import io.h8.stages.{Behavior, State}

case object CompleteStage extends TestStage[Any, Any] {
  def apply(in: Any): State[Any, Any] = State.Yield(in, () => Behavior.Complete, _ => Behavior.Complete)

  def apply[I]: TestStage[I, I] = CompleteStage.asInstanceOf[TestStage[I, I]]
}
