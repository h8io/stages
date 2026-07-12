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
      Gen.zip(
        Gen.nonEmptyListOf(Arbitrary.arbitrary[StatusAndEvolutionToYield[Long, String, Throwable]]),
        Gen.long,
        Arbitrary.arbitrary[Status.Complete[Throwable]])) {
      case (yieldSuppliers, in, complete) =>
        val initial = mock[Stage[Long, String, Throwable]]("initial stage")
        val evolved = createStage(yieldSuppliers.tail, initial, in)
        val lastYield = genYield[Long, String, Throwable]("last", yieldSuppliers.head, complete)
        val resultStage = mock[Stage[Long, String, Throwable]]("result stage")
        inSequence {
          (evolved.apply _).expects(in).returns(lastYield)
          (lastYield.evolution.evolve _).expects(complete).returns(resultStage)
          val yld = Repeat(initial)(in)
          val expectedStatus = if (complete.isEmpty) Status.Success else complete
          val evolution = inside((lastYield, yld)) {
            case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, `expectedStatus`, evolution)) =>
              resultOut shouldBe lastOut
              evolution
            case (Yield.None(_, _), Yield.None(`expectedStatus`, evolution)) => evolution
          }
          evolution.evolve(Status.Success) shouldBe Repeat(resultStage)
          evolution.evolve(mockComplete()) shouldBe Repeat(resultStage)
          (lastYield.evolution.dispose _).expects()
          noException should be thrownBy evolution.dispose()
        }
    }

  @tailrec private def createStage[I, O, E](
      yieldSuppliers: List[StatusAndEvolutionToYield[I, O, E]],
      stage: Stage[I, O, E], in: I): Stage[I, O, E] =
    yieldSuppliers match {
      case head :: tail =>
        val id = yieldSuppliers.length.toString
        val yld = genYield[I, O, E](id, head, Status.Success)
        val updated = mock[Stage[I, O, E]](s"stage $id")
        (yld.evolution.evolve _).expects(Status.Success).returns(updated)
        (stage.apply _).expects(in).returns(yld)
        createStage(tail, updated, in)
      case Nil => stage
    }

  private def genYield[I, O, E](
      id: String,
      yieldSupplier: StatusAndEvolutionToYield[I, O, E], status: Status[E]): Yield[I, O, E] =
    yieldSupplier(status, mock[Evolution[I, O, E]](s"evolution $id"))

  it should "call the alterand.skip() method and evolve the skipped alterand on Success" in {
    val stage = mock[Stage[UUID, String, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, String, Exception]]("evolution")
    val skipped = mock[Stage[UUID, String, Exception]]("skipped stage")
    inSequence {
      (stage.skip _).expects().returns(evolution)
      (evolution.evolve _).expects(Status.Success).returns(skipped)
      val result = Repeat(stage).skip()
      testConstEvolution(result, Repeat(skipped))
      (evolution.dispose _).expects()
      noException should be thrownBy result.dispose()
    }
  }
}
