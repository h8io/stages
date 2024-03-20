package io.h8.stages

sealed trait Behavior[-I, +O] {
  def kind: BehaviorKind
  def stage: Stage[I, O]
  def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O] = (previous.kind & kind)(previous.stage ~> stage)
}

object Behavior {
  case object Complete extends Behavior[Any, Nothing] {
    def kind: BehaviorKind = BehaviorKind.Complete
    def stage: Stage[Any, Nothing] = ???
  }

  final case class Redo[-I, +O](stage: Stage[I, O]) extends Behavior[I, O] {
    override def kind: BehaviorKind = BehaviorKind.Redo
  }

  final case class Undefined[-I, +O](stage: Stage[I, O]) extends Behavior[I, O] {
    override def kind: BehaviorKind = BehaviorKind.Undefined
  }
}
