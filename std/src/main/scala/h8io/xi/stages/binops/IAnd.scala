package h8io.xi.stages.binops

import h8io.xi.stages
import h8io.xi.stages.{Stage, Yield}

final case class IAnd[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOp[I, LO, RO, (LO, RO), E] {
  def apply(in: I): Yield[I, (LO, RO), E] =
    (left(in), right(in)) match {
      case (Yield.Some(leftOut, leftSignal, leftOnDone), Yield.Some(rightOut, rightSignal, rightOnDone)) =>
        Yield.Some((leftOut, rightOut), leftSignal ++ rightSignal, IAnd.OnDone(leftOnDone, rightOnDone))
      case (left, right) => Yield.None(left.signal ++ right.signal, IAnd.OnDone(left.onDone, right.onDone))
    }
}

object IAnd {
  private final case class OnDone[-I, +LO, +RO, +E](left: stages.OnDone[I, LO, E], right: stages.OnDone[I, RO, E])
      extends stages.OnDone[I, (LO, RO), E] {
    def onSuccess(): Stage[I, (LO, RO), E] = IAnd(left.onSuccess(), right.onSuccess())
    def onComplete(): Stage[I, (LO, RO), E] = IAnd(left.onComplete(), right.onComplete())
    def onError(): Stage[I, (LO, RO), E] = IAnd(left.onError(), right.onError())
  }
}
