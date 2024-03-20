package io.h8.stages

sealed trait BehaviorKind {
  def apply[I, O](stage: => Stage[I, O]): Behavior[I, O]
  def &(other: BehaviorKind): BehaviorKind
}

object BehaviorKind {
  case object Complete extends BehaviorKind {
    def apply[I, O](stage: => Stage[I, O]): Behavior[I, O] = Behavior.Complete
    def &(other: BehaviorKind): BehaviorKind = Complete
  }

  case object Redo extends BehaviorKind {
    def apply[I, O](stage: => Stage[I, O]): Behavior[I, O] = Behavior.Redo(stage)

    override def &(other: BehaviorKind): BehaviorKind = other match {
      case `Complete` => Complete
      case _ => Redo
    }
  }

  case object Undefined extends BehaviorKind {
    def apply[I, O](stage: => Stage[I, O]): Behavior[I, O] = Behavior.Undefined(stage)
    override def &(other: BehaviorKind): BehaviorKind = other
  }
}