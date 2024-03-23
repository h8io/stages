package io.h8.stages.test

import io.h8.stages.{Behavior, Stage, State}

final case object CompleteStage extends Stage[Any, Any] {
  def apply(in: Any): State[Any, Any] = State.Yield(in, () => Behavior.Complete, _ => Behavior.Complete)

  def apply[I]: Stage[I, I] = CompleteStage.asInstanceOf[Stage[I, I]]
}
