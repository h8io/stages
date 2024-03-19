package io.h8.stages.util

import io.h8.stages.{Behavior, Stage, State}

class Identity[T] extends Stage.Safe[T, T] {
  override def apply(in: T): State[T, T] =
    State.Yield(in, () => Behavior.Undefined(this), _ => Behavior.Undefined(this))
}
