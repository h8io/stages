package h8io.stages.cycles

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
        Gen.listOf(Arbitrary.arbitrary[StatusAndEvolutionToYieldSome[String, String, String]]),
        Arbitrary.arbitrary[StatusAndEvolutionToYield[String, String, String]],
        Arbitrary.arbitrary[String],
        Arbitrary.arbitrary[Status.Complete[String]]
      )) { case (yieldSuppliers, lastYieldSupplier, in, complete) =>
      val initial = mock[Stage.Endo[String, String]]("initial stage")
      val (lastIn, evolved) = genStage(yieldSuppliers, initial, in)
      val lastEvolution = mock[Evolution[String, String, String]]("last evolution")
      val lastYield = lastYieldSupplier(complete, lastEvolution)
      val resultStage = mock[Stage.Endo[String, String]]("result stage")
      inSequence {
        (evolved.apply _).expects(lastIn).returns(lastYield)
        (lastEvolution.evolve _).expects(complete).returns(resultStage)
        val expectedStatus = if (complete.isEmpty) Status.Success else complete
        val evolution = inside((lastYield, Loop(initial)(in))) {
          case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, `expectedStatus`, evolution)) =>
            resultOut shouldBe lastOut
            evolution
          case (Yield.None(_, _), Yield.None(`expectedStatus`, evolution)) => evolution
        }
        evolution.evolve(Status.Success) shouldBe Loop(resultStage)
        evolution.evolve(mockComplete()) shouldBe Loop(resultStage)
        (lastEvolution.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
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
      val resultStage = mock[Stage.Endo[BigInt, Exception]]("result stage")

      inSequence {
        (evolved.apply _).expects(lastIn).returns(lastYield)
        (lastEvolution.evolve _).expects(Status.Success).returns(resultStage)
        val evolution = inside((lastYield, Loop(initial)(in))) {
          case (Yield.None(_, _), Yield.None(Status.Success, evolution)) => evolution
        }

        evolution.evolve(Status.Success) shouldBe Loop(resultStage)
        evolution.evolve(mockComplete()) shouldBe Loop(resultStage)
        (lastEvolution.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
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
        (evolution.evolve _).expects(Status.Success).returns(evolved)
        (stage.apply _).expects(in).returns(yld)
        genStage(tail, evolved, yld.out)
      case Nil => (in, stage)
    }

  it should "call the alterand.skip() method and evolve the skipped alterand on Success" in {
    val stage = mock[Stage.Endo[UUID, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, UUID, Exception]]("evolution")
    val skipped = mock[Stage.Endo[UUID, Exception]]("skipped stage")
    inSequence {
      (stage.skip _).expects().returns(evolution)
      (evolution.evolve _).expects(Status.Success).returns(skipped)
      val result = Loop(stage).skip()
      testConstEvolution(result, Loop(skipped))
      (evolution.dispose _).expects()
      noException should be thrownBy result.dispose()
    }
  }
}
