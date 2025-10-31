package h8io.xi.stages.alterations

import h8io.xi.stages.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.util.UUID
import scala.annotation.tailrec

class LoopTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries {
  "Loop" should "be executed until the signal is Complete" in
    forAll(
      Gen.zip(
        Gen.listOf(Arbitrary.arbitrary[SignalAndOnDoneToYieldSome[String, String, Nothing]]),
        Arbitrary.arbitrary[SignalAndOnDoneToYield[String, String, Nothing]],
        Arbitrary.arbitrary[String]
      )) { case (yieldSuppliers, lastYieldSupplier, in) =>
      val initial = mock[Stage.Endo[String, Nothing]]("initial stage")
      val (lastIn, evolved) = genStage(yieldSuppliers, initial, in)
      val lastOnDone = mock[OnDone[String, String, Nothing]]("last onDone")
      val lastYield = lastYieldSupplier(Signal.Complete, lastOnDone)
      (evolved.apply _).expects(lastIn).returns(lastYield)
      val resultStage = mock[Stage.Endo[String, Nothing]]("result stage")
      (lastOnDone.onComplete _).expects().returns(resultStage)
      val onDone = inside((lastYield, Loop(initial)(in))) {
        case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, Signal.Success, onDone)) =>
          resultOut shouldBe lastOut
          onDone
        case (Yield.None(_, _), Yield.None(Signal.Success, onDone)) => onDone
      }
      val expectedStage = Loop(resultStage)
      onDone.onSuccess() shouldBe expectedStage
      onDone.onComplete() shouldBe expectedStage
      onDone.onError() shouldBe expectedStage
    }

  it should "be executed until the signal is Error" in
    forAll(
      Gen.zip(
        Gen.listOf(Arbitrary.arbitrary[SignalAndOnDoneToYieldSome[UUID, UUID, String]]),
        Arbitrary.arbitrary[SignalAndOnDoneToYield[UUID, UUID, String]],
        Gen.uuid,
        Arbitrary.arbitrary[Signal.Error[String]]
      )) { case (yieldSuppliers, lastYieldSupplier, in, lastSignal) =>
      val initial = mock[Stage.Endo[UUID, String]]("initial stage")
      val (lastIn, evolved) = genStage(yieldSuppliers, initial, in)
      val lastOnDone = mock[OnDone[UUID, UUID, String]]
      val lastYield = lastYieldSupplier(lastSignal, lastOnDone)
      (evolved.apply _).expects(lastIn).returns(lastYield)
      val resultStage = mock[Stage.Endo[UUID, String]]("result stage")
      (lastOnDone.onError _).expects().returns(resultStage)
      val onDone = inside((lastYield, Loop(initial)(in))) {
        case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, `lastSignal`, onDone)) =>
          resultOut shouldBe lastOut
          onDone
        case (Yield.None(_, _), Yield.None(`lastSignal`, onDone)) => onDone
      }
      val expectedStage = Loop(resultStage)
      onDone.onSuccess() shouldBe expectedStage
      onDone.onComplete() shouldBe expectedStage
      onDone.onError() shouldBe expectedStage
    }

  it should "be executed until the result is None" in
    forAll(
      Gen.zip(
        Gen.listOf(Arbitrary.arbitrary[SignalAndOnDoneToYieldSome[BigInt, BigInt, Exception]]),
        Arbitrary.arbitrary[BigInt]
      )) { case (yieldSuppliers, in) =>
      val initial = mock[Stage.Endo[BigInt, Exception]]("initial stage")
      val (lastIn, evolved) = genStage(yieldSuppliers, initial, in)
      val lastOnDone = mock[OnDone[BigInt, BigInt, Exception]]("last OnDone")
      val lastYield = Yield.None(Signal.Success, lastOnDone)
      (evolved.apply _).expects(lastIn).returns(lastYield)
      val resultStage = mock[Stage.Endo[BigInt, Exception]]("result stage")
      (lastOnDone.onComplete _).expects().returns(resultStage)
      val onDone = inside((lastYield, Loop(initial)(in))) {
        case (Yield.None(_, _), Yield.None(Signal.Success, onDone)) => onDone
      }
      val expectedStage = Loop(resultStage)
      onDone.onSuccess() shouldBe expectedStage
      onDone.onComplete() shouldBe expectedStage
      onDone.onError() shouldBe expectedStage
    }

  @tailrec private def genStage[T: Arbitrary, E](
      yieldSuppliers: List[SignalAndOnDoneToYieldSome[T, T, E]],
      stage: Stage.Endo[T, E], in: T): (T, Stage.Endo[T, E]) =
    yieldSuppliers match {
      case head :: tail =>
        val id = yieldSuppliers.length.toString
        val onDone = mock[OnDone[T, T, E]](s"onDone $id")
        val yld = head(Signal.Success, onDone)
        val evolved = mock[Stage.Endo[T, E]](s"stage $id")
        (onDone.onSuccess _).expects().returns(evolved)
        (stage.apply _).expects(in).returns(yld)
        genStage(tail, evolved, yld.out)
      case Nil => (in, stage)
    }

  "dispose" should "call alterand's dispose" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]
    (alterand.dispose _).expects()
    noException should be thrownBy Loop(alterand).dispose()
  }
}
