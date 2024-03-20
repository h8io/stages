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
              val leftBehavior = leftOnSuccess()
              val rightBehavior = rightOnSuccess()
              (leftBehavior.kind & rightBehavior.kind)(SeqFork(leftBehavior.stage, rightBehavior.stage))
            },
            { failure =>
              val leftBehavior = leftOnFailure(failure)
              val rightBehavior = rightOnFailure(failure)
              (leftBehavior.kind & rightBehavior.kind)(SeqFork(leftBehavior.stage, rightBehavior.stage))
            }
          )
        case State.Done(rightOnSuccess) => State.Done(() => fromBoth(leftOnSuccess(), rightOnSuccess()))
        case rightFailure @ State.Failure(rightCause, rightOnFailure) =>
          State.Failure(rightCause, () => fromBoth(leftOnFailure(rightFailure), rightOnFailure()))
      }
    case State.Done(leftOnSuccess) => State.Done(() => fromLeft(leftOnSuccess()))
    case State.Failure(leftCause, leftOnFailure) => State.Failure(leftCause, () => fromLeft(leftOnFailure()))
  }

  private def fromLeft[II <: I, LOO >: LO](behavior: Behavior[II, LOO]): Behavior[II, (LOO, RO)] =
    behavior.kind(SeqFork(behavior.stage, right))

  private def fromBoth[II <: I, LOO >: LO, ROO >: RO](
      leftBehavior: Behavior[II, LOO],
      rightBehavior: Behavior[II, ROO]): Behavior[II, (LOO, ROO)] =
    (leftBehavior.kind & rightBehavior.kind)(SeqFork(leftBehavior.stage, rightBehavior.stage))
}