package io.h8.stages.util

import io.h8.stages.{Behavior, Stage, State}

import scala.annotation.tailrec

final case class Recursion[T](stage: Stage[T, T]) extends Stage[T, T] {
  @tailrec
  private def loop(current: Stage[T, T], previous: State.Yield[T, T]): State[T, T] = {
    val (state, behavior) = current.safe(previous.out).concluded()
    behavior match {
      case Behavior.Complete =>
        state match {
          case state: State.Failure[T, T, ?] => state
          case _: State.Done[T, T] => previous
          case state: State.Yield[T, T] => state
        }
      case Behavior.Redo(stage) =>
        state match {
          case _: State.Break[T, T] => loop(stage, previous)
          case state: State.Yield[T, T] => loop(stage, state)
        }
      case _: Behavior.Undefined[T, T] => state
    }
  }

  def apply(in: T): State[T, T] = loop(stage, State.Yield[T, T](in, () => Behavior.Complete, _ => Behavior.Complete))
}
