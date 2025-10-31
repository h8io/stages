package h8io.xi.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

trait StagesCoreTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  def onDoneMock[I, O, E](onDone: OnDone[I, O, E], signal: Signal[E], stage: Stage[I, O, E]): Unit =
    signal match {
      case Signal.Success => (onDone.onSuccess _).expects().returns(stage)
      case Signal.Complete => (onDone.onComplete _).expects().returns(stage)
      case _: Signal.Error[E] => (onDone.onError _).expects().returns(stage)
    }

  def testWrappedOnDone[II, IO, IE, OI, OO, OE](
      wrappedOnDone: OnDone[OI, OO, OE],
      onDone: OnDone[II, IO, IE],
      alteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]]): Unit =
    testWrappedOnDone(wrappedOnDone, onDone, alteration, alteration, alteration)

  def testWrappedOnDone[II, IO, IE, OI, OO, OE](
      wrappedOnDone: OnDone[OI, OO, OE],
      onDone: OnDone[II, IO, IE],
      onSuccessAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]],
      onCompleteAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]],
      onErrorAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]]): Unit = {
    val onSuccessStage = mock[Stage[II, IO, IE]]("onSuccess stage")
    (onDone.onSuccess _).expects().returns(onSuccessStage)
    wrappedOnDone.onSuccess() shouldBe onSuccessAlteration(onSuccessStage)

    val onCompleteStage = mock[Stage[II, IO, IE]]("onComplete stage")
    (onDone.onComplete _).expects().returns(onCompleteStage)
    wrappedOnDone.onComplete() shouldBe onCompleteAlteration(onCompleteStage)

    val onErrorStage = mock[Stage[II, IO, IE]]("onError stage")
    (onDone.onError _).expects().returns(onErrorStage)
    wrappedOnDone.onError() shouldBe onErrorAlteration(onErrorStage)
  }
}
