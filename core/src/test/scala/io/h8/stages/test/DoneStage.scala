package io.h8.stages.test

import io.h8.stages.{Conclusion, Stage, State}

object DoneStage extends Stage[Any, Nothing] {
  override def apply(v: Any): State[Any, Nothing] = State.Done(Conclusion.Complete)
}
