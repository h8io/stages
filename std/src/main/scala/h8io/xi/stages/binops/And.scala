package h8io.xi.stages.binops

import h8io.xi.stages.{OnDone, Stage, Yield}

final case class And[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOp[I, LO, RO, (LO, RO), E] {
  override def apply(in: I): Yield[I, (LO, RO), E] =
    left(in) match {
      case Yield.Some(leftOut, leftSignal, leftOnDone) => right(in) match {
          case Yield.Some(rightOut, rightSignal, rightOnDone) =>
            Yield.Some((leftOut, rightOut), leftSignal ++ rightSignal, And.BothOnDone(leftOnDone, rightOnDone))
          case Yield.None(rightSignal, rightOnDone) =>
            Yield.None(leftSignal ++ rightSignal, And.BothOnDone(leftOnDone, rightOnDone))
        }
      case Yield.None(signal, onDone) => Yield.None(signal, And.LeftOnDone(onDone, right))
    }
}

object And {
  private final case class LeftOnDone[-I, +LO, +RO, +E](leftOnDone: OnDone[I, LO, E], right: Stage[I, RO, E])
      extends OnDone[I, (LO, RO), E] {
    def onSuccess(): Stage[I, (LO, RO), E] = And(leftOnDone.onSuccess(), right)
    def onComplete(): Stage[I, (LO, RO), E] = And(leftOnDone.onComplete(), right)
    def onError(): Stage[I, (LO, RO), E] = And(leftOnDone.onError(), right)
  }

  private final case class BothOnDone[-I, +LO, +RO, +E](leftOnDone: OnDone[I, LO, E], rightOnDone: OnDone[I, RO, E])
      extends OnDone[I, (LO, RO), E] {
    def onSuccess(): Stage[I, (LO, RO), E] = And(leftOnDone.onSuccess(), rightOnDone.onSuccess())
    def onComplete(): Stage[I, (LO, RO), E] = And(leftOnDone.onComplete(), rightOnDone.onComplete())
    def onError(): Stage[I, (LO, RO), E] = And(leftOnDone.onError(), rightOnDone.onError())
  }
}
