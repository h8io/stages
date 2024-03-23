package io.h8.stages

sealed trait Behavior[-I, +O] {
  def map[II, OO](f: Stage[I, O] => Stage[II, OO]): Behavior[II, OO]
  def combine[II, OO, III, OOO](that: Behavior[II, OO])(
      f: (Stage[I, O], Stage[II, OO]) => Stage[III, OOO]): Behavior[III, OOO]
  final def <~[PI](previous: Behavior[PI, I]): Behavior[PI, O] = combine(previous)((c, p) => p ~> c)
}

object Behavior {
  case object Complete extends Behavior[Any, Nothing] {
    def map[II, OO](f: Stage[Any, Nothing] => Stage[II, OO]): Behavior[II, OO] = Complete

    def combine[II, OO, III, OOO](that: Behavior[II, OO])(
        f: (Stage[Any, Nothing], Stage[II, OO]) => Stage[III, OOO]): Behavior[III, OOO] = Complete
  }

  final case class Redo[-I, +O](stage: Stage[I, O]) extends Behavior[I, O] {
    def map[II, OO](f: Stage[I, O] => Stage[II, OO]): Behavior[II, OO] = Redo(f(stage))

    def combine[II, OO, III, OOO](that: Behavior[II, OO])(
        f: (Stage[I, O], Stage[II, OO]) => Stage[III, OOO]): Behavior[III, OOO] = that match {
      case `Complete` => Complete
      case that: Redo[II, OO] => Redo(f(stage, that.stage))
      case that: Undefined[II, OO] => Redo(f(stage, that.stage))
    }
  }

  final case class Undefined[-I, +O](stage: Stage[I, O]) extends Behavior[I, O] {
    def map[II, OO](f: Stage[I, O] => Stage[II, OO]): Behavior[II, OO] = Undefined(f(stage))

    def combine[II, OO, III, OOO](that: Behavior[II, OO])(
        f: (Stage[I, O], Stage[II, OO]) => Stage[III, OOO]): Behavior[III, OOO] = that match {
      case `Complete` => Complete
      case that: Redo[II, OO] => Redo(f(stage, that.stage))
      case that: Undefined[II, OO] => Undefined(f(stage, that.stage))
    }
  }
}
