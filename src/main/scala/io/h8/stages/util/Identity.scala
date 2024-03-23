package io.h8.stages.util

import io.h8.stages.{Behavior, Stage, State}

object Identity extends Stage.Safe[Any, Any] {
  override def apply(in: Any): State[Any, Any] =
    State.Yield(in, () => Behavior.Undefined(this), _ => Behavior.Undefined(this))

  def apply[T]: Stage.Safe[T, T] = Identity.asInstanceOf[Stage.Safe[T, T]]
}
