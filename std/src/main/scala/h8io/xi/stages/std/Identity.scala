package h8io.xi.stages.std

object Identity extends Fn.Endo[Any] {
  def apply[T]: Fn.Endo[T] = asInstanceOf[Fn.Endo[T]]

  def f(in: Any): Any = in
}
