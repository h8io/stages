package io.h8.stages.test

import io.h8.stages.{Behavior, State}

final case class RedoWhileStage[I](condition: I => Boolean) extends TestStage[I, I] {
  def apply(in: I): State[I, I] = {
    val next = if (condition(in)) Behavior.Redo(this) else Behavior.Complete
    State.Yield(in, () => next, _ => Behavior.Complete)
  }
}
