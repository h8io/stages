package io.h8.stages

sealed trait State[-I, +O] {
  private[stages] def <~[PI](previous: State.Yield[PI, I]): State[PI, O]

  def conclude: () => Behavior[I, O]
}

object State {
  final case class Yield[-I, +O](out: O, conclude: () => Behavior[I, O], onFailure: Failure[?, ?, ?] => Behavior[I, O])
      extends State[I, O] {
    private[stages] def <~[PI](previous: Yield[PI, I]): Yield[PI, O] = Yield(
      out,
      () => conclude() <~ previous.conclude(),
      failure => onFailure(failure) <~ previous.onFailure(failure))
  }

  sealed trait Break[-I, +O] extends State[I, O] {
    private[stages] def ~>[NO](next: => Behavior[O, NO]): Break[I, NO]
  }

  final case class Done[-I, +O](conclude: () => Behavior[I, O]) extends Break[I, O] {
    private[stages] def <~[PI](previous: Yield[PI, I]): Done[PI, O] = Done(() => conclude() <~ previous.conclude())

    private[stages] def ~>[NO](next: => Behavior[O, NO]): Done[I, NO] = Done(() => next <~ conclude())
  }

  final case class Failure[-I, +O, +E](cause: E, conclude: () => Behavior[I, O]) extends Break[I, O] {
    private[stages] def <~[PI](previous: Yield[PI, I]): State[PI, O] =
      Failure(cause, () => conclude() <~ previous.onFailure(this))

    private[stages] def ~>[NO](next: => Behavior[O, NO]): Break[I, NO] =
      Failure(cause, () => next <~ conclude())
  }

  def Defect(cause: Exception): Failure[Any, Nothing, Exception] = Failure(cause, () => Behavior.Complete)

  def UndefinedBehavior[I, O](state: State[I, O]): Failure[I, O, State[I, O]] =
    Failure[I, O, State[I, O]](state, () => Behavior.Complete)
}
