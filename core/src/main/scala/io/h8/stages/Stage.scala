package io.h8.stages

import io.h8.stages.State.Yield

import scala.annotation.tailrec

object Stage {
  def apply[I, O](f: I => O): Stage[I, O] = new Stage[I, O] {
    def apply(in: I): State[I, O] = Yield(f(in), _ => Conclusion.Undefined(this))
  }
}

trait Stage[-I, +O] extends (I => State[I, O]) {
  def ~>[OO](next: Stage[O, OO]): Stage[I, OO] =
    (in: I) =>
      apply(in) match {
        case state: State.Yield[I, O] => state <~ next(state.out)
        case state: State.Done[I, O] => state ~> next
      }

  final def ~>[OO](f: O => OO): Stage[I, OO] = this ~> Stage(f)

  @tailrec
  final def execute(in: I): Result[I, O] = {
    val result = apply(in).toResult
    result.conclusion match {
      case Conclusion.Redo(next) => next.execute(in)
      case _ => result
    }
  }
}
