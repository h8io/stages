package io.h8.stages.util

import io.h8.stages.{Stage, State}

final case class SeqFork[-I, +O1, +O2](stage1: Stage[I, O1], stage2: Stage[I, O2]) extends Stage.Safe[I, (O1, O2)] {
//  override def apply(in: I): State[I, (O1, O2)] =
//    stage1(in) match {
//      case State.Yield(out1, onSuccess1, onFailure1) => ???
//      case State.Done(onSuccess) => State.Done(() => ???)
//      case State.Failure(cause, onFailure) => ???
//    }
}
