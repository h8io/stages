package h8io.xi.stages.binops

import cats.data.Ior
import h8io.xi.stages
import h8io.xi.stages.{Stage, Yield}

final case class IOr[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOp[I, LO, RO, Ior[LO, RO], E] {
  def apply(in: I): Yield[I, Ior[LO, RO], E] =
    (left(in), right(in)) match {
      case (Yield.Some(leftOut, leftSignal, leftOnDone), Yield.Some(rightOut, rightSignal, rightOnDone)) =>
        Yield.Some(Ior.Both(leftOut, rightOut), leftSignal ++ rightSignal, IOr.OnDone(leftOnDone, rightOnDone))
      case (Yield.Some(leftOut, leftSignal, leftOnDone), Yield.None(rightSignal, rightOnDone)) =>
        Yield.Some(Ior.Left(leftOut), leftSignal ++ rightSignal, IOr.OnDone(leftOnDone, rightOnDone))
      case (Yield.None(leftSignal, leftOnDone), Yield.Some(rightOut, rightSignal, rightOnDone)) =>
        Yield.Some(Ior.Right(rightOut), leftSignal ++ rightSignal, IOr.OnDone(leftOnDone, rightOnDone))
      case (Yield.None(leftSignal, leftOnDone), Yield.None(rightSignal, rightOnDone)) =>
        Yield.None(leftSignal ++ rightSignal, IOr.OnDone(leftOnDone, rightOnDone))
    }
}

object IOr {
  private final case class OnDone[-I, +LO, +RO, +E](left: stages.OnDone[I, LO, E], right: stages.OnDone[I, RO, E])
      extends stages.OnDone[I, Ior[LO, RO], E] {
    def onSuccess(): Stage[I, Ior[LO, RO], E] = IOr(left.onSuccess(), right.onSuccess())
    def onComplete(): Stage[I, Ior[LO, RO], E] = IOr(left.onComplete(), right.onComplete())
    def onError(): Stage[I, Ior[LO, RO], E] = IOr(left.onError(), right.onError())
  }
}
