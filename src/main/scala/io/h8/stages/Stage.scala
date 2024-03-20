package io.h8.stages

import scala.annotation.tailrec

object Stage {
  def apply[I, O](f: I => O): Stage.Safe[I, O] = new Stage.Safe[I, O] {
    def apply(in: I): State[I, O] =
      try State.Yield(f(in), () => Behavior.Undefined(this), _ => Behavior.Undefined(this))
      catch {
        case e: Exception => State.Defect(e)
      }
  }

  trait Safe[-I, +O] extends Stage[I, O]

  private def safe[I, O](stage: Stage[I, O]): Safe[I, O] = stage match {
    case stage: Safe[I, O] => stage
    case stage =>
      (in: I) =>
        try stage(in)
        catch {
          case e: Exception => State.Defect(e)
        }
  }
}

trait Stage[-I, +O] extends (I => State[I, O]) {
  final def ~>[NO](next: Stage[O, NO]): Stage.Safe[I, NO] = (in: I) =>
    Stage.safe(this)(in) match {
      case state: State.Yield[I, O] => Stage.safe(next)(state.out) <~ state
      case state: State.Break[I, O] => state ~> Behavior.Undefined(next)
    }

  final def ~>[NO](f: O => NO): Stage[I, NO] = ~>(Stage(f))

  final def &&[II <: I, OO](that: Stage[II, OO]): Stage[II, (O, OO)] = util.SeqFork(this, that)

  @tailrec
  final def execute(in: I): State[I, O] = {
    val state = Stage.safe(this)(in)
    (state match {
      case failure: State.Failure[I, O, ?] => failure.onFailure()
      case success: State.Success[I, O] => success.onSuccess()
    }) match {
      case Behavior.Complete => state
      case Behavior.Redo(next) => next.execute(in)
      case _ => State.UndefinedBehavior
    }
  }

  final def loop(in: I)(implicit ev: O <:< I): State[I, O] = {
    @tailrec
    def loop(current: Stage[I, O], previous: State[I, O], in: I): State[I, O] = Stage.safe(current)(in) match {
      case state: State.Failure[I, O, ?] => state
      case _: State.Done[I, O] => previous
      case state @ State.Yield(out, onSuccess, _) =>
        onSuccess() match {
          case Behavior.Redo(next) => loop(next, state, out)
          case _ => state
        }
    }
    loop(this, State.Done[I, O](() => Behavior.Complete), in)
  }
}
