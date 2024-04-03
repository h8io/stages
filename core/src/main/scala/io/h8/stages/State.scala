package io.h8.stages

sealed trait State[-I, +O] {
  def toResult: Result[I, O]
}

sealed trait Result[-I, +O] {
  def conclusion: Conclusion[I, O]
}

object State {
  final case class Done[-I, +O](conclusion: Conclusion[I, O]) extends State[I, O] with Result[I, O] {
    def ~>[OO](stage: Stage[O, OO]): Done[I, OO] = Done(conclusion <~ Conclusion.Undefined(stage))

    override def toResult: Result[I, O] = this
  }

  final case class Yield[-I, +O](out: O, conclude: Option[Conclusion[?, ?]] => Conclusion[I, O]) extends State[I, O] {
    def <~[OO](previous: State[O, OO]): State[I, OO] = previous match {
      case Done(conclusion) => Done(conclude(Some(conclusion)) <~ conclusion)
      case Yield(out, conclude) =>
        Yield(
          out,
          { conclusion =>
            val previousConclusion = conclude(conclusion)
            this.conclude(Some(previousConclusion)) <~ previousConclusion
          })
    }

    override def toResult: Result[I, O] = Result.Yield(out, conclude(None))
  }
}

object Result {
  type Done[-I, +O] = State.Done[I, O]

  final case class Yield[-I, +O](out: O, conclusion: Conclusion[I, O]) extends Result[I, O]
}
