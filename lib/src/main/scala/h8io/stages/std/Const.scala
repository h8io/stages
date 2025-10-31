package h8io.stages.std

import h8io.stages.Stage

final case class Const[+O](out: O) extends Fn[Any, O] with Stage[Any, O, Nothing] {
  override def f(in: Any): O = out
}
