package io.h8.stages.test

import io.h8.stages.{Conclusion, Stage, State}

case object CompleteStage extends Stage[Any, Any] {
  def apply(in: Any): State[Any, Any] = State.Yield(in, (_: Option[Conclusion[?, ?]]) => Conclusion.Complete)

  def apply[I]: Stage[I, I] = CompleteStage.asInstanceOf[Stage[I, I]]
}
