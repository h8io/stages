package h8io.stages.operators

import h8io.stages.*
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
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "Loop" should "be executed until the status is Complete" in
    forAll(
      Gen.zip(
        Gen.listOf(Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[String, String, Nothing]]),
        Arbitrary.arbitrary[StatusAndEvolutionToYield[String, String, Nothing]],
        Arbitrary.arbitrary[String]
      )) { case (yieldSuppliers, lastYieldSupplier, in) =>
      val initial = mock[Stage.Endo[String, Nothing]]("initial stage")
      val (lastIn, evolved) = genStage(yieldSuppliers, initial, in)
      val lastEvolution = mock[Evolution[String, String, Nothing]]("last evolution")
      val lastYield = lastYieldSupplier(Status.Complete, lastEvolution)
      (evolved.apply _).expects(lastIn).returns(lastYield)
      val resultStage = mock[Stage.Endo[String, Nothing]]("result stage")
      (lastEvolution.onComplete _).expects().returns(resultStage)
      val evolution = inside((lastYield, Loop(initial)(in))) {
        case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, Status.Success, evolution)) =>
          resultOut shouldBe lastOut
          evolution
        case (Yield.None(_, _), Yield.None(Status.Success, evolution)) => evolution
      }
      val expectedStage = Loop(resultStage)
      evolution.onSuccess() shouldBe expectedStage
      evolution.onComplete() shouldBe expectedStage
      evolution.onError() shouldBe expectedStage
    }

  it should "be executed until the status is Error" in
    forAll(
      Gen.zip(
        Gen.listOf(Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[UUID, UUID, String]]),
        Arbitrary.arbitrary[StatusAndEvolutionToYield[UUID, UUID, String]],
        Gen.uuid,
        Arbitrary.arbitrary[Status.Error[String]]
      )) { case (yieldSuppliers, lastYieldSupplier, in, lastStatus) =>
      val initial = mock[Stage.Endo[UUID, String]]("initial stage")
      val (lastIn, evolved) = genStage(yieldSuppliers, initial, in)
      val lastEvolution = mock[Evolution[UUID, UUID, String]]
      val lastYield = lastYieldSupplier(lastStatus, lastEvolution)
      (evolved.apply _).expects(lastIn).returns(lastYield)
      val resultStage = mock[Stage.Endo[UUID, String]]("result stage")
      (lastEvolution.onError _).expects().returns(resultStage)
      val evolution = inside((lastYield, Loop(initial)(in))) {
        case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, `lastStatus`, evolution)) =>
          resultOut shouldBe lastOut
          evolution
        case (Yield.None(_, _), Yield.None(`lastStatus`, evolution)) => evolution
      }
      val expectedStage = Loop(resultStage)
      evolution.onSuccess() shouldBe expectedStage
      evolution.onComplete() shouldBe expectedStage
      evolution.onError() shouldBe expectedStage
    }

  it should "be executed until the result is None" in
    forAll(
      Gen.zip(
        Gen.listOf(Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[BigInt, BigInt, Exception]]),
        Arbitrary.arbitrary[BigInt]
      )) { case (yieldSuppliers, in) =>
      val initial = mock[Stage.Endo[BigInt, Exception]]("initial stage")
      val (lastIn, evolved) = genStage(yieldSuppliers, initial, in)
      val lastEvolution = mock[Evolution[BigInt, BigInt, Exception]]("last evolution")
      val lastYield = Yield.None(Status.Success, lastEvolution)
      (evolved.apply _).expects(lastIn).returns(lastYield)
      val resultStage = mock[Stage.Endo[BigInt, Exception]]("result stage")
      (lastEvolution.onComplete _).expects().returns(resultStage)
      val evolution = inside((lastYield, Loop(initial)(in))) {
        case (Yield.None(_, _), Yield.None(Status.Success, evolution)) => evolution
      }
      val expectedStage = Loop(resultStage)
      evolution.onSuccess() shouldBe expectedStage
      evolution.onComplete() shouldBe expectedStage
      evolution.onError() shouldBe expectedStage
    }

  @tailrec private def genStage[T: Arbitrary, E](
      yieldSuppliers: List[StatusAndEvolutionToYieldSome[T, T, E]],
      stage: Stage.Endo[T, E], in: T): (T, Stage.Endo[T, E]) =
    yieldSuppliers match {
      case head :: tail =>
        val id = yieldSuppliers.length.toString
        val evolution = mock[Evolution[T, T, E]](s"evolution $id")
        val yld = head(Status.Success, evolution)
        val evolved = mock[Stage.Endo[T, E]](s"stage $id")
        (evolution.onSuccess _).expects().returns(evolved)
        (stage.apply _).expects(in).returns(yld)
        genStage(tail, evolved, yld.out)
      case Nil => (in, stage)
    }

  it should "call the alterand.skip() method" in {
    val stage = mock[Stage.Endo[UUID, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, UUID, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testAlteredEvolution(Loop(stage).skip(), evolution, Loop[UUID, Exception])
  }

  "dispose" should "call alterand's dispose" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]
    (alterand.dispose _).expects()
    noException should be thrownBy Loop(alterand).dispose()
  }
}
