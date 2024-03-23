package io.h8.stages.util

import io.h8.stages.{Behavior, Stage, State}

case class RedoWhileStage[I](condition: I => Boolean) extends Stage[I, I] {
  def apply(in: I): State[I, I] = {
    val next = if (condition(in)) Behavior.Redo(this) else Behavior.Complete
    State.Yield(in, () => next,  _ => Behavior.Complete)
  }
}
