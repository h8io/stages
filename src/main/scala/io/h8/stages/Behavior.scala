package io.h8.stages

sealed trait Behavior[-I, +O] {
  private[stages] def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O]

  def update[II, OO](stage: Stage[II, OO]): Behavior[II, OO]
}

object Behavior {
  case object Complete extends Behavior[Any, Nothing] {
    private[stages] def <~[PI](previous: Behavior[PI, Any]): Behavior[PI, Nothing] = this

    override def update[II, OO](stage: Stage[II, OO]): Behavior[II, OO] = this
  }

  final case class Redo[-I, +O](stage: Stage[I, O]) extends Behavior[I, O] {
    private[stages] def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O] = previous match {
      case `Complete` => Complete
      case Redo(previous) => Redo(previous ~> stage)
      case Undefined(previous) => Redo(previous ~> stage)
    }

    override def update[II, OO](stage: Stage[II, OO]): Behavior[II, OO] = Redo(stage)
  }

  final case class Undefined[-I, +O](stage: Stage[I, O]) extends Behavior[I, O] {
    private[stages] def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O] = previous match {
      case `Complete` => Complete
      case Redo(previous) => Redo(previous ~> stage)
      case Undefined(previous) => Undefined(previous ~> stage)
    }

    override def update[II, OO](stage: Stage[II, OO]): Behavior[II, OO] = Undefined(stage)
  }
}
