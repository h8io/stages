package h8io.stages.alterations

import h8io.stages.base.{BaseAlterator, BaseEvolution, TryStage}
import h8io.stages.{Stage, Status, Yield}

final case class Try[-I, +O, +E](alterand: Stage[I, O, E])
    extends BaseAlterator[Stage[I, O, E], I, O, Either[Throwable, E]]
    with TryStage[I, O, Either[Throwable, E]]
    with BaseEvolution[I, O, Either[Throwable, E]] {
  override def body(in: I): Yield[I, O, Either[Throwable, E]] =
    alterand(in).map(identity, _.map(Right(_)), _.map(Try(_)))

  override def recover(in: I, e: Throwable): Yield[I, O, Either[Throwable, E]] =
    Yield.None(Status.Error(Left(e)), this)
}
