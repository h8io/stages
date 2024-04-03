package io.h8.stages

case object Identity extends Stage[Any, Any] {
  override def apply(in: Any): State[Any, Any] = State.Yield(in, _ => Conclusion.Undefined(this))

  def apply[T]: Stage[T, T] = asInstanceOf[Stage[T, T]]
}
