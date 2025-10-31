package h8io.xi.stages.std

import h8io.xi.stages.Stage

final case class Const[+O](out: O) extends Fn[Any, O] with Stage[Any, O, Nothing] {
  override def f(in: Any): O = out
}
