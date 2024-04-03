package io.h8.stages

sealed trait Conclusion[-I, +O] {
  final def <~[OO](previous: Conclusion[O, OO]): Conclusion[I, OO] = combine(previous)(_ ~> _)

  def map[II, OO](f: Stage[I, O] => Stage[II, OO]): Conclusion[II, OO]

  def combine[II, OO, III, OOO](that: Conclusion[II, OO])(
      f: (Stage[I, O], Stage[II, OO]) => Stage[III, OOO]): Conclusion[III, OOO]
}

object Conclusion {
  final case class Undefined[-I, +O](stage: Stage[I, O]) extends Conclusion[I, O] {
    override def map[II, OO](f: Stage[I, O] => Stage[II, OO]): Undefined[II, OO] = Undefined(f(stage))

    override def combine[II, OO, III, OOO](that: Conclusion[II, OO])(
        f: (Stage[I, O], Stage[II, OO]) => Stage[III, OOO]): Conclusion[III, OOO] = that match {
      case previous: Break => previous
      case Redo(stage) => Redo(f(this.stage, stage))
      case Undefined(stage) => Undefined(f(this.stage, stage))
    }
  }

  final case class Redo[I, O](stage: Stage[I, O]) extends Conclusion[I, O] {
    override def map[II, OO](f: Stage[I, O] => Stage[II, OO]): Redo[II, OO] = Redo(f(stage))

    override def combine[II, OO, III, OOO](that: Conclusion[II, OO])(
        f: (Stage[I, O], Stage[II, OO]) => Stage[III, OOO]): Conclusion[III, OOO] = that match {
      case that: Break => that
      case Redo(stage) => Redo(f(this.stage, stage))
      case Undefined(stage) => Redo(f(this.stage, stage))
    }
  }

  trait Break extends Conclusion[Any, Nothing]

  case object Complete extends Break {
    override def map[II, OO](f: Stage[Any, Nothing] => Stage[II, OO]): this.type = this

    override def combine[II, OO, III, OOO](that: Conclusion[II, OO])(
        f: (Stage[Any, Nothing], Stage[II, OO]) => Stage[III, OOO]): Conclusion[III, OOO] = that match {
      case that: Failure => that
      case _ => Complete
    }
  }

  final case class Failure(causes: List[?]) extends Break {
    override def map[II, OO](f: Stage[Any, Nothing] => Stage[II, OO]): Failure = this

    override def combine[II, OO, III, OOO](that: Conclusion[II, OO])(
        f: (Stage[Any, Nothing], Stage[II, OO]) => Stage[III, OOO]): Conclusion[III, OOO] = that match {
      case Failure(causes) => Failure(this.causes ++ causes)
      case _ => this
    }
  }
}
