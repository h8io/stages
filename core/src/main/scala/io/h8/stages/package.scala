package io.h8

import scala.annotation.tailrec

package object stages {
  implicit class SymmetricStage[T](val stage: Stage[T, T]) extends AnyVal {
    final def loop(in: T): Result[T, T] = loop(Result.Yield[T, T](in, Conclusion.Complete), stage)

    @tailrec
    private def loop(in: Result.Yield[T, T], stage: Stage[T, T]): Result[T, T] = {
      val result = stage.apply(in.out).toResult
      result match {
        case result: Result.Yield[T, T] => result.conclusion match {
          case _: Conclusion.Break => result
          case Conclusion.Undefined(stage) => loop(result, stage)
          case Conclusion.Redo(stage) => loop(result, stage)
        }
        case State.Done(conclusion) => conclusion match {
          case _: Conclusion.Failure => result
          case _ => in
        }
      }
    }
  }
}
