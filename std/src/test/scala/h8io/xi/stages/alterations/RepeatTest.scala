package h8io.xi.stages.alterations

import h8io.xi.stages.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant
import java.util.UUID
import scala.annotation.tailrec

class RepeatTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries {
  "Repeat" should "be executed until the signal is Complete" in
    forAll(Gen.zip(Gen.nonEmptyListOf(Arbitrary.arbitrary[SignalAndOnDoneToYield[Long, String, Nothing]]), Gen.long)) {
      case (yieldSuppliers, in) =>
        val initial = mock[Stage[Long, String, Nothing]]("initial stage")
        val evolved = createStage(yieldSuppliers.tail, initial, in)
        val lastYield = genYield[Long, String, Nothing]("last", yieldSuppliers.head, Signal.Complete)
        (evolved.apply _).expects(in).returns(lastYield)
        val resultStage = mock[Stage[Long, String, Nothing]]("result stage")
        (lastYield.onDone.onComplete _).expects().returns(resultStage)
        val onDone = inside((lastYield, Repeat(initial)(in))) {
          case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, Signal.Success, onDone)) =>
            resultOut shouldBe lastOut
            onDone
          case (Yield.None(_, _), Yield.None(Signal.Success, onDone)) => onDone
        }
        val expectedStage = Repeat(resultStage)
        onDone.onSuccess() shouldBe expectedStage
        onDone.onComplete() shouldBe expectedStage
        onDone.onError() shouldBe expectedStage
    }

  it should "be executed until the signal is Error" in
    forAll(
      Gen.zip(
        Gen.nonEmptyListOf(Arbitrary.arbitrary[SignalAndOnDoneToYield[Instant, UUID, Exception]]),
        Arbitrary.arbitrary[Instant],
        Arbitrary.arbitrary[Signal.Error[Exception]]
      )) {
      case (yieldSuppliers, in, lastSignal) =>
        val initial = mock[Stage[Instant, UUID, Exception]]("initial stage")
        val evolved = createStage(yieldSuppliers.tail, initial, in)
        val lastYield = genYield[Instant, UUID, Exception]("last", yieldSuppliers.head, lastSignal)
        (evolved.apply _).expects(in).returns(lastYield)
        val resultStage = mock[Stage[Instant, UUID, Exception]]("result stage")
        (lastYield.onDone.onError _).expects().returns(resultStage)
        val onDone = inside((lastYield, Repeat(initial)(in))) {
          case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, `lastSignal`, onDone)) =>
            resultOut shouldBe lastOut
            onDone
          case (Yield.None(_, _), Yield.None(`lastSignal`, onDone)) => onDone
        }
        val expectedStage = Repeat(resultStage)
        onDone.onSuccess() shouldBe expectedStage
        onDone.onComplete() shouldBe expectedStage
        onDone.onError() shouldBe expectedStage
    }

  @tailrec private def createStage[I, O, E](
      yieldSuppliers: List[SignalAndOnDoneToYield[I, O, E]],
      stage: Stage[I, O, E], in: I): Stage[I, O, E] =
    yieldSuppliers match {
      case head :: tail =>
        val id = yieldSuppliers.length.toString
        val yld = genYield[I, O, E](id, head, Signal.Success)
        val updated = mock[Stage[I, O, E]](s"stage $id")
        (yld.onDone.onSuccess _).expects().returns(updated)
        (stage.apply _).expects(in).returns(yld)
        createStage(tail, updated, in)
      case Nil => stage
    }

  private def genYield[I, O, E](
      id: String,
      yieldSupplier: SignalAndOnDoneToYield[I, O, E], signal: Signal[E]): Yield[I, O, E] =
    yieldSupplier(signal, mock[OnDone[I, O, E]](s"onDone $id"))

  "dispose" should "call alterand's dispose" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]
    (alterand.dispose _).expects()
    noException should be thrownBy Repeat(alterand).dispose()
  }
}
