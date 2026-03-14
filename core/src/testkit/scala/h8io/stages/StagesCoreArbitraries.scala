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

  type StatusAndOnDoneToYieldSome[I, O, E] = (Status[E], OnDone[I, O, E]) => Yield.Some[I, O, E]

  implicit def arbStatusAndOnDoneToYieldSome[I, O: Arbitrary, E]: Arbitrary[StatusAndOnDoneToYieldSome[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[O] map { out => Yield.Some(out, _: Status[E], _: OnDone[I, O, E]) })

  type StatusAndOnDoneToYield[I, O, E] = (Status[E], OnDone[I, O, E]) => Yield[I, O, E]

  implicit def arbStatusAndOnDoneToYield[I, O: Arbitrary, E]: Arbitrary[StatusAndOnDoneToYield[I, O, E]] =
    Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[StatusAndOnDoneToYieldSome[I, O, E]],
        Gen.const(Yield.None[I, O, E](_: Status[E], _: OnDone[I, O, E]))))

  type OnDoneToYieldSome[I, O, E] = OnDone[I, O, E] => Yield.Some[I, O, E]

  implicit def arbOnDoneToYieldSome[I, O: Arbitrary, E: Arbitrary]: Arbitrary[OnDoneToYieldSome[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndOnDoneToYieldSome[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: OnDone[I, O, E])
      })

  type OnDoneToYieldNone[I, O, E] = OnDone[I, O, E] => Yield.None[I, O, E]

  implicit def arbOnDoneToYieldNone[I, O, E: Arbitrary]: Arbitrary[OnDoneToYieldNone[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[Status[E]].map(status => Yield.None(status, _: OnDone[I, O, E])))

  type OnDoneToYield[I, O, E] = OnDone[I, O, E] => Yield[I, O, E]

  implicit def arbOnDoneToYield[I, O: Arbitrary, E: Arbitrary]: Arbitrary[OnDoneToYield[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Status[E]], Arbitrary.arbitrary[StatusAndOnDoneToYield[I, O, E]]).map {
        case (status, yieldSupplier) => yieldSupplier(status, _: OnDone[I, O, E])
      })
}
