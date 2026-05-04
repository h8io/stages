package h8io.stages.base

import h8io.stages.{Status, Yield}

/** Base trait for stages that extract a value from a container type `C`.
  *
  * A `Projection` either succeeds (`h8io.stages.Yield.Some`) when the expected value is present, or produces a
  * `h8io.stages.Yield.None` (with `h8io.stages.Status.Success`) when it is absent — preserving the overall pipeline
  * status without signaling an error.
  *
  * Helper values [[some]] and [[none]] are pre-built with `Status.Success` and `this` as the evolution, ready for use
  * in `apply` implementations of concrete projections.
  *
  * @tparam I
  *   the container input type (contravariant)
  * @tparam O
  *   the extracted value type
  */
trait Projection[-I, O] extends BaseStage[I, O, Nothing] {

  /** Creates a successful `h8io.stages.Yield.Some` carrying `out`. */
  protected def some(out: O): Yield.Some[I, O, Nothing] = Yield.Some(out, Status.Success, this.toEvolution)

  /** A pre-built `h8io.stages.Yield.None` used when the container holds no value for this projection.
    */
  protected val none: Yield.None[I, O, Nothing] = Yield.None(Status.Success, this.toEvolution)
}

/** A [[Projection]] for the left side of a covariant binary type constructor `C[+_, +_]`.
  *
  * The singleton instance operates on `C[Any, ?]` and can be safely cast to any specific `Projection[C[T, ?], T]` via
  * the `apply[T]` method.
  *
  * @tparam C
  *   the binary type constructor (must be covariant in both type parameters)
  */
trait LeftProjection[C[+_, +_]] extends Projection[C[Any, ?], Any] {

  /** Returns a typed view of this projection for left-side values of type `T`.
    *
    * The cast is safe because `C` is covariant in both parameters and the projection only reads (never writes) the
    * container.
    *
    * @tparam T
    *   the concrete left-side type
    * @return
    *   this projection typed as `Projection[C[T, ?], T]`
    */
  final def apply[T]: Projection[C[T, ?], T] = asInstanceOf[Projection[C[T, ?], T]]
}

/** A [[Projection]] for the right side of a covariant binary type constructor `C[+_, +_]`.
  *
  * Mirror of [[LeftProjection]] for the right type parameter.
  *
  * @tparam C
  *   the binary type constructor (must be covariant in both type parameters)
  */
trait RightProjection[C[+_, +_]] extends Projection[C[?, Any], Any] {

  /** Returns a typed view of this projection for right-side values of type `T`.
    *
    * @tparam T
    *   the concrete right-side type
    * @return
    *   this projection typed as `Projection[C[?, T], T]`
    */
  final def apply[T]: Projection[C[?, T], T] = asInstanceOf[Projection[C[?, T], T]]
}
