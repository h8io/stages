package h8io.stages

import org.scalacheck.{Arbitrary, Gen}

/** ScalaCheck generators for the core types, shared with the other modules through the `core % "test->testkit"`
  * dependency.
  *
  * A [[Yield]] cannot be generated on its own: its evolution has to be the one the test is about — a mock, usually,
  * with its own expectations. The generators therefore produce ''partially applied'' yields, leaving the evolution (and
  * sometimes the status) to be supplied at the point of use, and each shape gets a type alias so that the implicit can
  * be summoned by name.
  */
trait StagesCoreArbitraries {

  /** A `Complete` with any number of errors, including none. */
  implicit def arbStatusComplete[E: Arbitrary]: Arbitrary[Status.Complete[E]] =
    Arbitrary(Gen.listOf(Arbitrary.arbitrary[E]).map(errors => Status.Complete(errors)))

  /** Either branch of [[Status]]: `Success` or a generated `Complete`. */
  implicit def arbStatus[E: Arbitrary]: Arbitrary[Status[E]] =
    Arbitrary(Gen.oneOf(Gen.const(Status.Success: Status[E]), arbStatusComplete[E].arbitrary))

  /** A [[Yield.Some]] awaiting its status and evolution — the output is already drawn. */
  type StatusAndEvolutionToYieldSome[I, O, E] = (Status[E], Evolution[I, O, E]) => Yield.Some[I, O, E]

  implicit def arbStatusAndEvolutionToYieldSome[I, O: Arbitrary, E]: Arbitrary[StatusAndEvolutionToYieldSome[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[O] map { out => Yield.Some(out, _: Status[E], _: Evolution[I, O, E]) })

  /** A [[Yield]] of either variant awaiting its status and evolution. */
  type StatusAndEvolutionToYield[I, O, E] = (Status[E], Evolution[I, O, E]) => Yield[I, O, E]

  implicit def arbStatusAndEvolutionToYield[I, O: Arbitrary, E]: Arbitrary[StatusAndEvolutionToYield[I, O, E]] =
    Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[I, O, E]],
        Gen.const(Yield.None[I, O, E](_: Status[E], _: Evolution[I, O, E]))))

  /** A [[Yield.Some]] awaiting only its evolution — the status is drawn along with the output. */
  type EvolutionToYieldSome[I, O, E] = Evolution[I, O, E] => Yield.Some[I, O, E]

  implicit def arbEvolutionToYieldSome[I, O: Arbitrary, E: Arbitrary]: Arbitrary[EvolutionToYieldSome[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: Evolution[I, O, E])
      })

  /** A [[Yield.None]] awaiting only its evolution. */
  type EvolutionToYieldNone[I, O, E] = Evolution[I, O, E] => Yield.None[I, O, E]

  implicit def arbEvolutionToYieldNone[I, O, E: Arbitrary]: Arbitrary[EvolutionToYieldNone[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[Status[E]].map(status => Yield.None(status, _: Evolution[I, O, E])))

  /** A [[Yield]] of either variant awaiting only its evolution. */
  type EvolutionToYield[I, O, E] = Evolution[I, O, E] => Yield[I, O, E]

  implicit def arbEvolutionToYield[I, O: Arbitrary, E: Arbitrary]: Arbitrary[EvolutionToYield[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndEvolutionToYield[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: Evolution[I, O, E])
      })
}
