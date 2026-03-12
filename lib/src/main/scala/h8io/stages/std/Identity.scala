package h8io.stages.std

import h8io.stages.base.Fn

object Identity extends Fn.Endo[Any] {
  def apply[T]: Fn.Endo[T] = asInstanceOf[Fn.Endo[T]]

  def f(in: Any): Any = in
}
