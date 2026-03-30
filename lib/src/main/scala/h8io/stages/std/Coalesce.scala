package h8io.stages.std

import h8io.stages.base.Fn

object Coalesce extends Fn[Either[Any, Any], Any] {
  override def f(input: Either[Any, Any]): Any =
    input match {
      case Left(value) => value
      case Right(value) => value
    }

  def apply[T]: Fn[Either[T, T], T] = asInstanceOf[Fn[Either[T, T], T]]
}
