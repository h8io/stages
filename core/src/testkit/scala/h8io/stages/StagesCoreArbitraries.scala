package h8io.stages

import org.scalacheck.{Arbitrary, Gen}

/** ScalaCheck `Arbitrary` instances for the core types of the `stages` library.
  *
  * Mix this trait into a test suite or a shared arbitraries object to bring the implicit generators into scope for
  * property-based tests.
  *
  * The trait provides generators for:
  *   - `Status` and `Status.Error`
  *   - Functions that produce `Yield.Some`, `Yield.None`, or either `Yield` variant, parameterised by `Status` and/or
  *     `Evolution`.
  *
  * The function-typed aliases (e.g. [[StatusAndEvolutionToYieldSome]]) make it possible to generate partially applied
  * `Yield` constructors and compose them into larger generators.
  */
trait StagesCoreArbitraries {

  /** Generates an arbitrary `Status.Complete` value with at least one error drawn from the implicit `Arbitrary[E]`
    * instance.
    *
    * @tparam E
    *   the error type; requires an implicit `Arbitrary[E]`
    */
  implicit def arbStatusComplete[E: Arbitrary]: Arbitrary[Status.Complete[E]] =
    Arbitrary(
      Gen.nonEmptyListOf(Arbitrary.arbitrary[E]).map(errors => Status.Complete(errors)))

  /** Generates an arbitrary `Status` by randomly selecting one of `Status.Success`, `Status.complete`, or a generated
    * `Status.Complete` with non-empty errors.
    *
    * @tparam E
    *   the error type; requires an implicit `Arbitrary[E]`
    */
  implicit def arbStatus[E: Arbitrary]: Arbitrary[Status[E]] =
    Arbitrary(
      Gen.oneOf(
        Gen.const(Status.Success: Status[E]),
        Gen.const(Status.SuccessfulComplete: Status[E]),
        arbStatusComplete[E].arbitrary))

  /** A function type that constructs a `Yield.Some` given a `Status` and an `Evolution`.
    *
    * Useful as a building block for generators that produce `Yield.Some` values with independently controlled status
    * and evolution.
    */
  type StatusAndEvolutionToYieldSome[I, O, E] = (Status[E], Evolution[I, O, E]) => Yield.Some[I, O, E]

  /** Generates a [[StatusAndEvolutionToYieldSome]] function by drawing a random output value `O` and partially applying
    * `Yield.Some`.
    *
    * @tparam I
    *   the stage input type (no `Arbitrary` required — captured as a phantom type)
    * @tparam O
    *   the output type; requires an implicit `Arbitrary[O]`
    * @tparam E
    *   the error type
    */
  implicit def arbStatusAndEvolutionToYieldSome[I, O: Arbitrary, E]: Arbitrary[StatusAndEvolutionToYieldSome[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[O] map { out => Yield.Some(out, _: Status[E], _: Evolution[I, O, E]) })

  /** A function type that constructs either a `Yield.Some` or a `Yield.None` given a `Status` and an `Evolution`.
    */
  type StatusAndEvolutionToYield[I, O, E] = (Status[E], Evolution[I, O, E]) => Yield[I, O, E]

  /** Generates a [[StatusAndEvolutionToYield]] function, randomly choosing between a `Some`-producing function (via
    * [[arbStatusAndEvolutionToYieldSome]]) and the `Yield.None` constructor.
    *
    * @tparam I
    *   the stage input type
    * @tparam O
    *   the output type; requires an implicit `Arbitrary[O]`
    * @tparam E
    *   the error type
    */
  implicit def arbStatusAndEvolutionToYield[I, O: Arbitrary, E]: Arbitrary[StatusAndEvolutionToYield[I, O, E]] =
    Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[I, O, E]],
        Gen.const(Yield.None[I, O, E](_: Status[E], _: Evolution[I, O, E]))))

  /** A function type that constructs a `Yield.Some` given only an `Evolution` (the `Status` is baked in).
    */
  type EvolutionToYieldSome[I, O, E] = Evolution[I, O, E] => Yield.Some[I, O, E]

  /** Generates an [[EvolutionToYieldSome]] function by drawing a random `Status` and a random
    * [[StatusAndEvolutionToYieldSome]], then partially applying the status.
    *
    * @tparam I
    *   the stage input type
    * @tparam O
    *   the output type; requires an implicit `Arbitrary[O]`
    * @tparam E
    *   the error type; requires an implicit `Arbitrary[E]`
    */
  implicit def arbEvolutionToYieldSome[I, O: Arbitrary, E: Arbitrary]: Arbitrary[EvolutionToYieldSome[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: Evolution[I, O, E])
      })

  /** A function type that constructs a `Yield.None` given only an `Evolution` (the `Status` is baked in).
    */
  type EvolutionToYieldNone[I, O, E] = Evolution[I, O, E] => Yield.None[I, O, E]

  /** Generates an [[EvolutionToYieldNone]] function by drawing a random `Status` and partially applying `Yield.None`.
    *
    * @tparam I
    *   the stage input type
    * @tparam O
    *   the (phantom) output type
    * @tparam E
    *   the error type; requires an implicit `Arbitrary[E]`
    */
  implicit def arbEvolutionToYieldNone[I, O, E: Arbitrary]: Arbitrary[EvolutionToYieldNone[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[Status[E]].map(status => Yield.None(status, _: Evolution[I, O, E])))

  /** A function type that constructs either a `Yield.Some` or a `Yield.None` given only an `Evolution` (the `Status` is
    * baked in).
    */
  type EvolutionToYield[I, O, E] = Evolution[I, O, E] => Yield[I, O, E]

  /** Generates an [[EvolutionToYield]] function by drawing a random `Status` and a random
    * [[StatusAndEvolutionToYield]], then partially applying the status.
    *
    * @tparam I
    *   the stage input type
    * @tparam O
    *   the output type; requires an implicit `Arbitrary[O]`
    * @tparam E
    *   the error type; requires an implicit `Arbitrary[E]`
    */
  implicit def arbEvolutionToYield[I, O: Arbitrary, E: Arbitrary]: Arbitrary[EvolutionToYield[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndEvolutionToYield[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: Evolution[I, O, E])
      })
}
