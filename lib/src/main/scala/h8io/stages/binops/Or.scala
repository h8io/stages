package h8io.stages.binops

import h8io.stages.base.BinaryOp
import h8io.stages.{OnDone, Stage, Yield}

final case class Or[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOp[I, LO, RO, Either[LO, RO], E] {
  override def apply(in: I): Yield[I, Either[LO, RO], E] =
    left(in) match {
      case Yield.Some(out, status, onDone) => Yield.Some(Left(out), status, Or.LeftOnDone(onDone, right))
      case Yield.None(leftStatus, leftOnDone) =>
        right(in) match {
          case Yield.Some(out, rightStatus, rightOnDone) =>
            Yield.Some(Right(out), leftStatus ++ rightStatus, Or.BothOnDone(leftOnDone, rightOnDone))
          case Yield.None(rightStatus, rightOnDone) =>
            Yield.None(leftStatus ++ rightStatus, Or.BothOnDone(leftOnDone, rightOnDone))
        }
    }
}

object Or {
  private final case class LeftOnDone[-I, +LO, +RO, +E](leftOnDone: OnDone[I, LO, E], right: Stage[I, RO, E])
      extends OnDone[I, Either[LO, RO], E] {
    override def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onSuccess(), right)
    override def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onComplete(), right)
    override def onError(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onError(), right)
  }

  private final case class BothOnDone[-I, +LO, +RO, +E](leftOnDone: OnDone[I, LO, E], rightOnDone: OnDone[I, RO, E])
      extends OnDone[I, Either[LO, RO], E] {
    override def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onSuccess(), rightOnDone.onSuccess())
    override def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onComplete(), rightOnDone.onComplete())
    override def onError(): Stage[I, Either[LO, RO], E] = Or(leftOnDone.onError(), rightOnDone.onError())
  }
}
