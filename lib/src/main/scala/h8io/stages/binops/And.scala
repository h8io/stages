package h8io.stages.binops

import h8io.stages.base.BinaryOp
import h8io.stages.{Evolution, Stage, Yield}

final case class And[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOp[I, LO, RO, (LO, RO), E] {
  override def apply(in: I): Yield[I, (LO, RO), E] =
    left(in) match {
      case Yield.Some(leftOut, leftStatus, leftEvolution) => right(in) match {
          case Yield.Some(rightOut, rightStatus, rightEvolution) =>
            Yield.Some((leftOut, rightOut), leftStatus ++ rightStatus, And.BothEvolution(leftEvolution, rightEvolution))
          case Yield.None(rightStatus, rightEvolution) =>
            Yield.None(leftStatus ++ rightStatus, And.BothEvolution(leftEvolution, rightEvolution))
        }
      case Yield.None(status, evolution) => Yield.None(status, And.LeftEvolution(evolution, right))
    }
}

object And {
  private final case class LeftEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E], right: Stage[I, RO, E])
      extends Evolution[I, (LO, RO), E] {
    override def onSuccess(): Stage[I, (LO, RO), E] = And(leftEvolution.onSuccess(), right)
    override def onComplete(): Stage[I, (LO, RO), E] = And(leftEvolution.onComplete(), right)
    override def onError(): Stage[I, (LO, RO), E] = And(leftEvolution.onError(), right)
  }

  private final case class BothEvolution[-I, +LO, +RO, +E](leftEvolution: Evolution[I, LO, E],
      rightEvolution: Evolution[I, RO, E])
      extends Evolution[I, (LO, RO), E] {
    override def onSuccess(): Stage[I, (LO, RO), E] = And(leftEvolution.onSuccess(), rightEvolution.onSuccess())
    override def onComplete(): Stage[I, (LO, RO), E] = And(leftEvolution.onComplete(), rightEvolution.onComplete())
    override def onError(): Stage[I, (LO, RO), E] = And(leftEvolution.onError(), rightEvolution.onError())
  }
}
