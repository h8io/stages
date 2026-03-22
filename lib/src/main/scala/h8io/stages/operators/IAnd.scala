package h8io.stages.operators

import h8io.stages
import h8io.stages.base.BinaryOperator
import h8io.stages.{Stage, Yield}

final case class IAnd[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOperator[Stage[I, LO, E], Stage[I, RO, E], I, (LO, RO), E] {
  override def apply(in: I): Yield[I, (LO, RO), E] =
    (left(in), right(in)) match {
      case (Yield.Some(leftOut, leftStatus, leftEvolution), Yield.Some(rightOut, rightStatus, rightEvolution)) =>
        Yield.Some((leftOut, rightOut), leftStatus ++ rightStatus, IAnd.Evolution(leftEvolution, rightEvolution))
      case (left, right) => Yield.None(left.status ++ right.status, IAnd.Evolution(left.evolution, right.evolution))
    }
}

object IAnd {
  private final case class Evolution[-I, +LO, +RO, +E](
      left: stages.Evolution[I, LO, E],
      right: stages.Evolution[I, RO, E])
      extends stages.Evolution[I, (LO, RO), E] {
    override def onSuccess(): Stage[I, (LO, RO), E] = IAnd(left.onSuccess(), right.onSuccess())
    override def onComplete(): Stage[I, (LO, RO), E] = IAnd(left.onComplete(), right.onComplete())
    override def onError(): Stage[I, (LO, RO), E] = IAnd(left.onError(), right.onError())
  }
}
