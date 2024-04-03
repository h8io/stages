package io.h8.stages.test

import io.h8.stages.{Conclusion, Stage, State}

final case class RedoWhileStage[I](condition: I => Boolean) extends Stage[I, I] {
  def apply(in: I): State[I, I] = {
    val next = if (condition(in)) Conclusion.Redo(this) else Conclusion.Complete
    State.Yield(in, _ => next)
  }
}
