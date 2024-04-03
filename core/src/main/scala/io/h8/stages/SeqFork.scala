package io.h8.stages

final case class SeqFork[-I, +LO, +RO](left: Stage[I, LO], right: Stage[I, RO]) extends Stage[I, (LO, RO)] {
  override def apply(in: I): State[I, (LO, RO)] = left(in) match {
    case State.Yield(leftOut, leftConclude) =>
      right(in) match {
        case State.Yield(rightOut, rightConclude) =>
          State.Yield(
            (leftOut, rightOut),
            { conclusion =>
              val rightConclusion = rightConclude(conclusion)
              val leftConclusion = leftConclude(conclusion)
              leftConclusion.combine(rightConclusion)(SeqFork(_, _))
            }
          )
        case State.Done(rightConclusion) => State.Done(fromBoth(rightConclusion, leftConclude(None)))
      }
    case State.Done(leftConclusion) => State.Done(fromLeft(leftConclusion))
  }

  private def fromLeft[II <: I, LOO >: LO](leftConclusion: Conclusion[II, LOO]): Conclusion[II, (LOO, RO)] =
    leftConclusion.map(SeqFork(_, right))

  private def fromBoth[II <: I, LOO >: LO, ROO >: RO](
      rightConclusion: Conclusion[II, ROO],
      leftConclusion: Conclusion[II, LOO]): Conclusion[II, (LOO, ROO)] =
    leftConclusion.combine(rightConclusion)(SeqFork(_, _))
}
