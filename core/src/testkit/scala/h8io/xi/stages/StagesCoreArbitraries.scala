package h8io.xi.stages

import org.scalacheck.{Arbitrary, Gen}

trait StagesCoreArbitraries {
  implicit def arbSignalError[E: Arbitrary]: Arbitrary[Signal.Error[E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[E], Arbitrary.arbitrary[List[E]]).map { case (head, tail) =>
        Signal.Error(head, tail)
      })

  implicit def arbSignal[E: Arbitrary]: Arbitrary[Signal[E]] =
    Arbitrary(Gen.oneOf(Gen.const(Signal.Success: Signal[E]), Gen.const(Signal.Complete), arbSignalError[E].arbitrary))

  type SignalAndOnDoneToYieldSome[I, O, E] = (Signal[E], OnDone[I, O, E]) => Yield.Some[I, O, E]

  implicit def arbSignalAndOnDoneToYieldSome[I, O: Arbitrary, E]: Arbitrary[SignalAndOnDoneToYieldSome[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[O] map { out => Yield.Some(out, _: Signal[E], _: OnDone[I, O, E]) })

  type SignalAndOnDoneToYield[I, O, E] = (Signal[E], OnDone[I, O, E]) => Yield[I, O, E]

  implicit def arbSignalAndOnDoneToYield[I, O: Arbitrary, E]: Arbitrary[SignalAndOnDoneToYield[I, O, E]] =
    Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[SignalAndOnDoneToYieldSome[I, O, E]],
        Gen.const(Yield.None[I, O, E](_: Signal[E], _: OnDone[I, O, E]))))

  type OnDoneToYieldSome[I, O, E] = OnDone[I, O, E] => Yield.Some[I, O, E]

  implicit def arbOnDoneToYieldSome[I, O: Arbitrary, E: Arbitrary]: Arbitrary[OnDoneToYieldSome[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Signal[E]], Arbitrary.arbitrary[SignalAndOnDoneToYieldSome[I, O, E]]).map {
        case (signal, yieldSupplier) => yieldSupplier(signal, _: OnDone[I, O, E])
      })

  type OnDoneToYieldNone[I, O, E] = OnDone[I, O, E] => Yield.None[I, O, E]

  implicit def arbOnDoneToYieldNone[I, O, E: Arbitrary]: Arbitrary[OnDoneToYieldNone[I, O, E]] =
    Arbitrary(Arbitrary.arbitrary[Signal[E]].map(signal => Yield.None(signal, _: OnDone[I, O, E])))

  type OnDoneToYield[I, O, E] = OnDone[I, O, E] => Yield[I, O, E]

  implicit def arbOnDoneToYield[I, O: Arbitrary, E: Arbitrary]: Arbitrary[OnDoneToYield[I, O, E]] =
    Arbitrary(
      Gen.zip(Arbitrary.arbitrary[Signal[E]], Arbitrary.arbitrary[SignalAndOnDoneToYield[I, O, E]]).map {
        case (signal, yieldSupplier) => yieldSupplier(signal, _: OnDone[I, O, E])
      })
}
