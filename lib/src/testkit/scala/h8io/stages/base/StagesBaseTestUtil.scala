package h8io.stages.base

import h8io.stages.{Evolution, Stage}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

trait StagesBaseTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  def testWrappedEvolution[II, IO, IE, OI, OO, OE](
      wrappedEvolution: Evolution[OI, OO, OE],
      evolution: Evolution[II, IO, IE],
      alteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]]): Unit =
    testWrappedEvolution(wrappedEvolution, evolution, alteration, alteration, alteration)

  def testWrappedEvolution[II, IO, IE, OI, OO, OE](
      wrappedEvolution: Evolution[OI, OO, OE],
      evolution: Evolution[II, IO, IE],
      onSuccessAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]],
      onCompleteAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]],
      onErrorAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]]): Unit = {
    val onSuccessStage = mock[Stage[II, IO, IE]]("onSuccess stage")
    (evolution.onSuccess _).expects().returns(onSuccessStage)
    wrappedEvolution.onSuccess() shouldBe onSuccessAlteration(onSuccessStage)

    val onCompleteStage = mock[Stage[II, IO, IE]]("onComplete stage")
    (evolution.onComplete _).expects().returns(onCompleteStage)
    wrappedEvolution.onComplete() shouldBe onCompleteAlteration(onCompleteStage)

    val onErrorStage = mock[Stage[II, IO, IE]]("onError stage")
    (evolution.onError _).expects().returns(onErrorStage)
    wrappedEvolution.onError() shouldBe onErrorAlteration(onErrorStage)
  }
}
