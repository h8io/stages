package io.h8.stages.util

import io.h8.stages.{Behavior, Stage, State}

final case class SeqFork[-I, +LO, +RO](left: Stage[I, LO], right: Stage[I, RO]) extends Stage.Safe[I, (LO, RO)] {
  override def apply(in: I): State[I, (LO, RO)] = left(in) match {
    case State.Yield(leftOut, leftOnSuccess, leftOnFailure) =>
      right(in) match {
        case State.Yield(rightOut, rightOnSuccess, rightOnFailure) =>
          State.Yield(
            (leftOut, rightOut),
            { () =>
              val rightBehavior = rightOnSuccess()
              val leftBehavior = leftOnSuccess()
              leftBehavior.combine(rightBehavior)(SeqFork(_, _))
            },
            { failure =>
              val rightBehavior = rightOnFailure(failure)
              val leftBehavior = leftOnFailure(failure)
              leftBehavior.combine(rightBehavior)(SeqFork(_, _))
            }
          )
        case State.Done(rightOnSuccess) => State.Done(() => fromBoth(rightOnSuccess(), leftOnSuccess()))
        case rightFailure @ State.Failure(rightCause, rightOnFailure) =>
          State.Failure(rightCause, () => fromBoth(rightOnFailure(), leftOnFailure(rightFailure)))
      }
    case State.Done(leftOnSuccess) => State.Done(() => fromLeft(leftOnSuccess()))
    case State.Failure(leftCause, leftOnFailure) => State.Failure(leftCause, () => fromLeft(leftOnFailure()))
  }

  private def fromLeft[II <: I, LOO >: LO](leftBehavior: Behavior[II, LOO]): Behavior[II, (LOO, RO)] =
    leftBehavior.map(SeqFork(_, right))

  private def fromBoth[II <: I, LOO >: LO, ROO >: RO](
      rightBehavior: Behavior[II, ROO],
      leftBehavior: Behavior[II, LOO]): Behavior[II, (LOO, ROO)] =
    leftBehavior.combine(rightBehavior)(SeqFork(_, _))
}
