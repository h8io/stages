package io.h8.stages

sealed trait Behavior[-I, +O] {
  private[stages] def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O]
}

object Behavior {
  case object Complete extends Behavior[Any, Nothing] {
    private[stages] def <~[PI](previous: Behavior[PI, Any]): Behavior[PI, Nothing] = this
  }

  final case class Redo[-I, +O](current: Stage[I, O]) extends Behavior[I, O] {
    private[stages] def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O] = previous match {
      case `Complete` => Complete
      case Redo(previous) => Redo(previous ~> current)
      case Undefined(previous) => Redo(previous ~> current)
    }
  }

  final case class Undefined[-I, +O](current: Stage[I, O]) extends Behavior[I, O] {
    private[stages] def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O] = previous match {
      case `Complete` => Complete
      case Redo(previous) => Redo(previous ~> current)
      case Undefined(previous) => Undefined(previous ~> current)
    }
  }
}
