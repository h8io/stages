package h8io.stages.cats

import cats.{Eq, Monoid}
import h8io.stages.Status

/** Cats typeclass instances for `h8io.stages.Status`.
  *
  * `Status` forms a `cats.Monoid` with `h8io.stages.Status.Success` as the identity element and
  * `h8io.stages.Status.combine` as the binary operation.
  *
  * Import `StatusInstances.*` to bring the instances into scope:
  * {{{
  * import h8io.stages.cats.StatusInstances.*
  * }}}
  */
object StatusInstances {
  implicit def statusMonoid[E]: Monoid[Status[E]] =
    new Monoid[Status[E]] {
      override def empty: Status[E] = Status.Success
      override def combine(x: Status[E], y: Status[E]): Status[E] = x.combine(y)
    }

  implicit def statusEq[E]: Eq[Status[E]] = Eq.fromUniversalEquals
}
