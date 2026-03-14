package h8io.stages.std

import h8io.stages.base.Fn

final case class Const[+O](out: O) extends Fn[Any, O] {
  override def f(in: Any): O = out
}
