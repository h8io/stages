package h8io.xi.stages.binops

import h8io.xi.stages.{OnDone, Stage, Yield}

final case class Or[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOp[I, LO, RO, Either[LO, RO], E] {
  def apply(in: I): Yield[I, Either[LO, RO], E] =
    left(in) match {
      case Yield.Some(out, signal, onDone) => Yield.Some(Left(out), signal, Or.LeftOnDone(onDone, right))
      case Yield.None(leftSignal, leftOnDone) =>
        right(in) match {
          case Yield.Some(out, rightSignal, rightOnDone) =>
            Yield.Some(Right(out), leftSignal ++ rightSignal, Or.BothOnDone(leftOnDone, rightOnDone))
          case Yield.None(rightSignal, rightOnDone) =>
            Yield.None(leftSignal ++ rightSignal, Or.BothOnDone(leftOnDone, rightOnDone))
        }
    }
}

object Or {
  private final case class LeftOnDone[-I, +LO, +RO, +E](leftOnDone: OnDone[I, LO, E], right: Stage[I, RO, E])
      extends OnDone[I, Either[LO, RO], E] {
    def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onSuccess(), right)
    def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onComplete(), right)
    def onError(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onError(), right)
  }

  private final case class BothOnDone[-I, +LO, +RO, +E](leftOnDone: OnDone[I, LO, E], rightOnDone: OnDone[I, RO, E])
      extends OnDone[I, Either[LO, RO], E] {
    def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onSuccess(), rightOnDone.onSuccess())
    def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onComplete(), rightOnDone.onComplete())
    def onError(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onError(), rightOnDone.onError())
  }
}
