package io.h8.stages.test

import io.h8.stages.{Behavior, Stage, State}

final case class RedoStage[-I, +O](count: Int, last: Stage[I, O]) extends Stage[I, O] {
  def apply(in: I): State[I, O] = {
    val next = if (count > 1) RedoStage(count - 1, last) else last
    State.Done(() => Behavior.Redo(next))
  }
}
