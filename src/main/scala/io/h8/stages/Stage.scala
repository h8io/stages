package io.h8.stages

import scala.annotation.tailrec

object Stage {
  def apply[I, O](f: I => O): Stage.Safe[I, O] = new Stage.Safe[I, O] {
    def apply(in: I): State[I, O] =
      try `yield`(f(in))
      catch {
        case e: Exception => State.Defect(e)
      }
  }

  trait Safe[-I, +O] extends Stage[I, O] {
    override def safe: Safe[I, O] = this
  }
}

trait Stage[-I, +O] extends (I => State[I, O]) {
  final def ~>[NO](next: Stage[O, NO]): Stage.Safe[I, NO] = (in: I) =>
    this.safe(in) match {
      case state: State.Yield[I, O] => next.safe(state.out) <~ state
      case state: State.Break[I, O] => state ~> Behavior.Undefined(next)
    }

  @inline final def ~>[NO](f: O => NO): Stage[I, NO] = ~>(Stage(f))

  @inline final def &&[II <: I, OO](that: Stage[II, OO]): Stage[II, (O, OO)] = util.SeqFork(this, that)

  @tailrec
  final def execute(in: I): State[I, O] = {
    val state = safe(in)
    (state match {
      case failure: State.Failure[I, O, ?] => failure.onFailure()
      case success: State.Success[I, O] => success.onSuccess()
    }) match {
      case Behavior.Complete => state.withBehavior(Behavior.Complete)
      case Behavior.Redo(next) => next.execute(in)
      case behavior @ Behavior.Undefined(stage) => State.UndefinedBehavior(state.withBehavior(behavior), stage)
    }
  }

  final def recursion[T >: O <: I](in: T): State[T, T] = {
    @tailrec
    def loop(current: Stage[T, T], previous: State.Yield[T, T]): State[T, T] = current.safe(previous.out) match {
      case state: State.Failure[T, T, ?] => state.onFailure() match {
        case Behavior.Complete => state.withBehavior(Behavior.Complete)
        case Behavior.Redo(stage) => loop(stage, previous)
        case behavior: Behavior.Undefined[T, T] => state.withBehavior(behavior)
      }
      case state: State.Done[T, T] =>
        state.onSuccess() match {
          case Behavior.Complete => previous
          case Behavior.Redo(stage) => loop(stage, previous)
          case behavior: Behavior.Undefined[T, T] => state.withBehavior(behavior)
        }
      case state @ State.Yield(_, onSuccess, _) =>
        onSuccess() match {
          case Behavior.Complete => state.withBehavior(Behavior.Complete)
          case behavior @ Behavior.Redo(next) => loop(next, state.withBehavior(behavior))
          case behavior: Behavior.Undefined[T, T] => state.withBehavior(behavior)
        }
    }
    loop(this, State.Yield[T, T](in, () => Behavior.Complete, _ => Behavior.Complete))
  }

  def safe: Stage.Safe[I, O] = (in: I) =>
    try this(in)
    catch {
      case e: Exception => State.Defect(e)
    }

  def `yield`[OO >: O](v: OO): State.Yield[I, OO] =
    State.Yield(v, () => Behavior.Undefined(this), _ => Behavior.Undefined(this))
}
