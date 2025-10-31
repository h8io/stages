package h8io.xi.stages.projections

import h8io.xi.stages.{Signal, Stage, Yield}

trait Projection[-I, O] extends Stage[I, O, Nothing] {
  protected def some(out: O): Yield.Some[I, O, Nothing] = Yield.Some(out, Signal.Success, this)
  protected val none: Yield.None[I, O, Nothing] = Yield.None(Signal.Success, this)
}

trait LeftProjection[C[+_, +_]] extends Projection[C[Any, ?], Any] {
  final def apply[T]: Projection[C[T, ?], T] = asInstanceOf[Projection[C[T, ?], T]]
}

trait RightProjection[C[+_, +_]] extends Projection[C[?, Any], Any] {
  final def apply[T]: Projection[C[?, T], T] = asInstanceOf[Projection[C[?, T], T]]
}
