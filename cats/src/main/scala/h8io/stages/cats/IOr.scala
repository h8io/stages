package h8io.stages.cats

import cats.data.Ior
import h8io.stages
import h8io.stages.base.{BinaryOp, LeftProjection, RightProjection}
import h8io.stages.{Stage, Yield}

final case class IOr[-I, +LO, +RO, +E](left: Stage[I, LO, E], right: Stage[I, RO, E])
    extends BinaryOp[I, LO, RO, Ior[LO, RO], E] {
  override def apply(in: I): Yield[I, Ior[LO, RO], E] =
    (left(in), right(in)) match {
      case (Yield.Some(leftOut, leftStatus, leftEvolution), Yield.Some(rightOut, rightStatus, rightEvolution)) =>
        Yield.Some(Ior.Both(leftOut, rightOut), leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
      case (Yield.Some(leftOut, leftStatus, leftEvolution), Yield.None(rightStatus, rightEvolution)) =>
        Yield.Some(Ior.Left(leftOut), leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
      case (Yield.None(leftStatus, leftEvolution), Yield.Some(rightOut, rightStatus, rightEvolution)) =>
        Yield.Some(Ior.Right(rightOut), leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
      case (Yield.None(leftStatus, leftEvolution), Yield.None(rightStatus, rightEvolution)) =>
        Yield.None(leftStatus ++ rightStatus, IOr.Evolution(leftEvolution, rightEvolution))
    }
}

object IOr {
  private final case class Evolution[-I, +LO, +RO, +E](
      left: stages.Evolution[I, LO, E],
      right: stages.Evolution[I, RO, E])
      extends stages.Evolution[I, Ior[LO, RO], E] {
    override def onSuccess(): Stage[I, Ior[LO, RO], E] = IOr(left.onSuccess(), right.onSuccess())
    override def onComplete(): Stage[I, Ior[LO, RO], E] = IOr(left.onComplete(), right.onComplete())
    override def onError(): Stage[I, Ior[LO, RO], E] = IOr(left.onError(), right.onError())
  }

  object Left extends LeftProjection[Ior] {
    override def apply(in: Ior[Any, ?]): Yield[Ior[Any, ?], Any, Nothing] =
      in.fold(out => some(out), _ => none, (out, _) => some(out))
  }

  object Right extends RightProjection[Ior] {
    override def apply(in: Ior[?, Any]): Yield[Ior[?, Any], Any, Nothing] =
      in.fold(_ => none, out => some(out), (_, out) => some(out))
  }
}
