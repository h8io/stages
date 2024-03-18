package io.h8.stages

class Identity[T] extends Stage.Safe[T, T] {
  override def apply(in: T): State[T, T] =
    State.Yield(in, () => Behavior.Undefined(this), _ => Behavior.Undefined(this))
}
