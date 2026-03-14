package h8io.stages.base

import h8io.stages.{Status, Yield}

trait Projection[-I, O] extends StageWithEvolution[I, O, Nothing] {
  protected def some(out: O): Yield.Some[I, O, Nothing] = Yield.Some(out, Status.Success, this)
  protected val none: Yield.None[I, O, Nothing] = Yield.None(Status.Success, this)
}

trait LeftProjection[C[+_, +_]] extends Projection[C[Any, ?], Any] {
  final def apply[T]: Projection[C[T, ?], T] = asInstanceOf[Projection[C[T, ?], T]]
}

trait RightProjection[C[+_, +_]] extends Projection[C[?, Any], Any] {
  final def apply[T]: Projection[C[?, T], T] = asInstanceOf[Projection[C[?, T], T]]
}
