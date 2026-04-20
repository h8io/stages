package h8io.stages.operators

import h8io.stages.*
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
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "Repeat" should "be executed until the status is Complete" in
    forAll(
      Gen.zip(Gen.nonEmptyListOf(Arbitrary.arbitrary[StatusAndEvolutionToYield[Long, String, Nothing]]), Gen.long)) {
      case (yieldSuppliers, in) =>
        val initial = mock[Stage[Long, String, Nothing]]("initial stage")
        val evolved = createStage(yieldSuppliers.tail, initial, in)
        val lastYield = genYield[Long, String, Nothing]("last", yieldSuppliers.head, Status.Complete)
        (evolved.apply _).expects(in).returns(lastYield)
        val resultStage = mock[Stage[Long, String, Nothing]]("result stage")
        (lastYield.evolution.onComplete _).expects().returns(resultStage)
        val evolution = inside((lastYield, Repeat(initial)(in))) {
          case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, Status.Success, evolution)) =>
            resultOut shouldBe lastOut
            evolution
          case (Yield.None(_, _), Yield.None(Status.Success, evolution)) => evolution
        }
        val expectedStage = Repeat(resultStage)
        evolution.onSuccess() shouldBe expectedStage
        evolution.onComplete() shouldBe expectedStage
        evolution.onError() shouldBe expectedStage
    }

  it should "be executed until the status is Error" in
    forAll(
      Gen.zip(
        Gen.nonEmptyListOf(Arbitrary.arbitrary[StatusAndEvolutionToYield[Instant, UUID, Exception]]),
        Arbitrary.arbitrary[Instant],
        Arbitrary.arbitrary[Status.Error[Exception]]
      )) {
      case (yieldSuppliers, in, lastStatus) =>
        val initial = mock[Stage[Instant, UUID, Exception]]("initial stage")
        val evolved = createStage(yieldSuppliers.tail, initial, in)
        val lastYield = genYield[Instant, UUID, Exception]("last", yieldSuppliers.head, lastStatus)
        (evolved.apply _).expects(in).returns(lastYield)
        val resultStage = mock[Stage[Instant, UUID, Exception]]("result stage")
        (lastYield.evolution.onError _).expects().returns(resultStage)
        val evolution = inside((lastYield, Repeat(initial)(in))) {
          case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, `lastStatus`, evolution)) =>
            resultOut shouldBe lastOut
            evolution
          case (Yield.None(_, _), Yield.None(`lastStatus`, evolution)) => evolution
        }
        val expectedStage = Repeat(resultStage)
        evolution.onSuccess() shouldBe expectedStage
        evolution.onComplete() shouldBe expectedStage
        evolution.onError() shouldBe expectedStage
    }

  @tailrec private def createStage[I, O, E](
      yieldSuppliers: List[StatusAndEvolutionToYield[I, O, E]],
      stage: Stage[I, O, E], in: I): Stage[I, O, E] =
    yieldSuppliers match {
      case head :: tail =>
        val id = yieldSuppliers.length.toString
        val yld = genYield[I, O, E](id, head, Status.Success)
        val updated = mock[Stage[I, O, E]](s"stage $id")
        (yld.evolution.onSuccess _).expects().returns(updated)
        (stage.apply _).expects(in).returns(yld)
        createStage(tail, updated, in)
      case Nil => stage
    }

  private def genYield[I, O, E](
      id: String,
      yieldSupplier: StatusAndEvolutionToYield[I, O, E], status: Status[E]): Yield[I, O, E] =
    yieldSupplier(status, mock[Evolution[I, O, E]](s"evolution $id"))

  it should "call the alterand.skip() method" in {
    val stage = mock[Stage[UUID, String, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, String, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testAlteredEvolution(Repeat(stage).skip(), evolution, Repeat[UUID, String, Exception])
  }
}
