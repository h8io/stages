package h8io.stages.binary

import h8io.stages.base.BaseBinaryStage
import h8io.stages.{Evolution, Stage, Yield}

final case class Or[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BaseBinaryStage[I, LO, RO, Either[LO, RO], E] {
  override def apply(in: I): Yield[I, Either[LO, RO], E] =
    left(in) match {
      case Yield.Some(out, status, evolution) => Yield.Some(Left(out), status, Or.LeftEvolution(evolution, right))
      case Yield.None(leftStatus, leftEvolution) =>
        right(in) match {
          case Yield.Some(out, rightStatus, rightEvolution) =>
            Yield.Some(Right(out), leftStatus ++ rightStatus, Or.BothEvolution(leftEvolution, rightEvolution))
          case Yield.None(rightStatus, rightEvolution) =>
            Yield.None(leftStatus ++ rightStatus, Or.BothEvolution(leftEvolution, rightEvolution))
        }
    }
}

object Or {
  private final case class LeftEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E], right: Stage[I, RO, E])
      extends Evolution[I, Either[LO, RO], E] {
    override def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onSuccess(), right)
    override def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onComplete(), right)
    override def onError(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onError(), right)
  }

  private final case class BothEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E],
      rightEvolution: Evolution[I, RO, E])
      extends Evolution[I, Either[LO, RO], E] {
    override def onSuccess(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onSuccess(), rightEvolution.onSuccess())
    override def onComplete(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onComplete(), rightEvolution.onComplete())
    override def onError(): Stage[I, Either[LO, RO], E] = Or(leftEvolution.onError(), rightEvolution.onError())
  }
}
