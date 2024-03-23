package io.h8.stages

sealed trait State[-I, +O] {
  private[stages] def <~[PI](previous: State.Yield[PI, I]): State[PI, O]
}

object State {
  sealed trait Success[-I, +O] extends State[I, O] {
    def onSuccess: () => Behavior[I, O]
  }

  final case class Yield[-I, +O](out: O, onSuccess: () => Behavior[I, O], onFailure: Failure[?, ?, ?] => Behavior[I, O])
      extends Success[I, O] {
    private[stages] def <~[PI](previous: Yield[PI, I]): Yield[PI, O] = Yield(
      out,
      () => onSuccess() <~ previous.onSuccess(),
      failure => onFailure(failure) <~ previous.onFailure(failure))
  }

  sealed trait Break[-I, +O] extends State[I, O] {
    private[stages] def ~>[NO](next: => Behavior[O, NO]): Break[I, NO]
  }

  final case class Done[-I, +O](onSuccess: () => Behavior[I, O]) extends Success[I, O] with Break[I, O] {
    private[stages] def <~[PI](previous: Yield[PI, I]): Done[PI, O] = Done(() => onSuccess() <~ previous.onSuccess())

    private[stages] def ~>[NO](next: => Behavior[O, NO]): Done[I, NO] = Done(() => next <~ onSuccess())
  }

  final case class Failure[-I, +O, +E](cause: E, onFailure: () => Behavior[I, O]) extends Break[I, O] {
    override private[stages] def <~[PI](previous: Yield[PI, I]): State[PI, O] =
      Failure(cause, () => onFailure() <~ previous.onFailure(this))

    override private[stages] def ~>[NO](next: => Behavior[O, NO]): Break[I, NO] =
      Failure(cause, () => next <~ onFailure())
  }

  def Defect(cause: Exception): Failure[Any, Nothing, Exception] = Failure(cause, () => Behavior.Complete)

  def UndefinedBehavior[I, O](state: State[I, O], stage: Stage[I, O]): Failure[I, O, (State[I, O], Stage[I, O])] =
    Failure[I, O, (State[I, O], Stage[I, O])]((state, stage), () => Behavior.Complete)
}
