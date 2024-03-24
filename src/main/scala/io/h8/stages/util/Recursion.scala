package io.h8.stages.util

import io.h8.stages.{Behavior, Stage, State}

import scala.annotation.tailrec

final case class Recursion[T](stage: Stage[T, T]) extends Stage [T, T] {
  @tailrec
  private def loop(current: Stage[T, T], previous: State.Yield[T, T]): State[T, T] = current.safe(previous.out) match {
    case state: State.Failure[T, T, ?] => state.onFailure() match {
      case Behavior.Complete => state
      case Behavior.Redo(stage) => loop(stage, previous)
      case _: Behavior.Undefined[T, T] => state
    }
    case state: State.Done[T, T] =>
      state.onSuccess() match {
        case Behavior.Complete => previous
        case Behavior.Redo(stage) => loop(stage, previous)
        case _: Behavior.Undefined[T, T] => state
      }
    case state @ State.Yield(_, onSuccess, _) =>
      onSuccess() match {
        case Behavior.Complete => state
        case Behavior.Redo(next) => loop(next, state)
        case _: Behavior.Undefined[T, T] => state
      }
  }

  def apply(in: T): State[T, T] = loop(stage, State.Yield[T, T](in, () => Behavior.Complete, _ => Behavior.Complete))
}
