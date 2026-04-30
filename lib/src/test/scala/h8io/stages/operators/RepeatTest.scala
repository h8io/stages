package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.StageOps
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
        (evolved.apply _).expects(in).returns(lastYield)
        (lastYield.evolution.apply _).expects(complete).returns(resultStage)
        val yld = Repeat(initial)(in)
        val expectedStatus = if (complete.isEmpty) Status.Success else complete
        val evolution = inside((lastYield, yld)) {
          case (Yield.Some(lastOut, _, _), Yield.Some(resultOut, `expectedStatus`, evolution)) =>
            resultOut shouldBe lastOut
            evolution
          case (Yield.None(_, _), Yield.None(`expectedStatus`, evolution)) => evolution
        }
        evolution shouldBe Repeat(resultStage).toEvolution
    }

  @tailrec private def createStage[I, O, E](
      yieldSuppliers: List[StatusAndEvolutionToYield[I, O, E]],
      stage: Stage[I, O, E], in: I): Stage[I, O, E] =
    yieldSuppliers match {
      case head :: tail =>
        val id = yieldSuppliers.length.toString
        val yld = genYield[I, O, E](id, head, Status.Success)
        val updated = mock[Stage[I, O, E]](s"stage $id")
        (yld.evolution.apply _).expects(Status.Success).returns(updated)
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
    testMappedEvolution(Repeat(stage).skip(), evolution, Repeat[UUID, String, Exception])
  }
}
