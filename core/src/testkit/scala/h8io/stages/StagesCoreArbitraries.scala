package h8io.stages

import org.scalacheck.{Arbitrary, Gen}

trait StagesCoreArbitraries {
  implicit def arbStatusError[E: Arbitrary]: Arbitrary[Status.Error[E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[E], Arbitrary.arbitrary[List[E]]).map { case (head, tail) =>
        Status.Error(head, tail)
      })

  implicit def arbStatus[E: Arbitrary]: Arbitrary[Status[E]] =
    Arbitrary(Gen.oneOf(Gen.const(Status.Success: Status[E]), Gen.const(Status.Complete), arbStatusError[E].arbitrary))

  type StatusAndEvolutionToYieldSome[I, O, E] = (Status[E], Evolution[I, O, E]) => Yield.Some[I, O, E]

  implicit def arbStatusAndEvolutionToYieldSome[I, O: Arbitrary, E]: Arbitrary[StatusAndEvolutionToYieldSome[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[O] map { out => Yield.Some(out, _: Status[E], _: Evolution[I, O, E]) })

  type StatusAndEvolutionToYield[I, O, E] = (Status[E], Evolution[I, O, E]) => Yield[I, O, E]

  implicit def arbStatusAndEvolutionToYield[I, O: Arbitrary, E]: Arbitrary[StatusAndEvolutionToYield[I, O, E]] =
    Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[I, O, E]],
        Gen.const(Yield.None[I, O, E](_: Status[E], _: Evolution[I, O, E]))))

  type EvolutionToYieldSome[I, O, E] = Evolution[I, O, E] => Yield.Some[I, O, E]

  implicit def arbEvolutionToYieldSome[I, O: Arbitrary, E: Arbitrary]: Arbitrary[EvolutionToYieldSome[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: Evolution[I, O, E])
      })

  type EvolutionToYieldNone[I, O, E] = Evolution[I, O, E] => Yield.None[I, O, E]

  implicit def arbEvolutionToYieldNone[I, O, E: Arbitrary]: Arbitrary[EvolutionToYieldNone[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[Status[E]].map(status => Yield.None(status, _: Evolution[I, O, E])))

  type EvolutionToYield[I, O, E] = Evolution[I, O, E] => Yield[I, O, E]

  implicit def arbEvolutionToYield[I, O: Arbitrary, E: Arbitrary]: Arbitrary[EvolutionToYield[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndEvolutionToYield[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: Evolution[I, O, E])
      })
}
