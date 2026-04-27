package h8io.stages

import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.util.UUID
import scala.concurrent.duration.Duration

class StatusTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "Success" should "be idempotent" in { Status.Success.combine(Status.Success) shouldBe Status.Success }

  "SucessfulComplete" should "be idempotent" in {
    Status.SuccessfulComplete.combine(Status.SuccessfulComplete) shouldBe Status.SuccessfulComplete
  }

  it should "be overridden by Complete with errors" in
    forAll((c: Status.Complete[String]) => Status.SuccessfulComplete.combine(c) shouldBe c)

  "Complete with errors" should "keep the order of causes in composition" in
    forAll { (previous: Status.Complete[String], next: Status.Complete[String]) =>
      previous.combine(next) shouldBe Status.Complete(previous.errors ++ next.errors)
    }

  it should "override complete" in
    forAll((c: Status.Complete[String]) => c.combine(Status.SuccessfulComplete) shouldBe c)

  it should "not be empty" in
    forAll { (c: Status.Complete[Exception]) =>
      c.isEmpty shouldBe false
      c.toList should contain theSameElementsInOrderAs c.errors
      val i = c.iterator
      i.toList should contain theSameElementsInOrderAs c.errors
    }

  "map" should "not change the Success status" in {
    Status.Success.map(mock[String => Long]) shouldBe Status.Success
  }

  it should "not change the complete status" in {
    Status.SuccessfulComplete.map(mock[Throwable => String]) shouldBe Status.SuccessfulComplete
  }

  it should "transform error values in Complete" in
    forAll(Gen.choose(1, 16)) { size =>
      forAll(Gen.zip(Gen.listOfN(size, Gen.uuid), Gen.listOfN(size, Gen.duration))) { case (initial, mapped) =>
        val initialComplete = Status.Complete(initial)
        val mappedComplete = Status.Complete(mapped)
        val f = mock[UUID => Duration]
        inSequence(
          for {
            (i, m) <- initial.zip(mapped)
          } (f.apply _).expects(i).returns(m)
        )
        initialComplete.map(f) shouldBe mappedComplete
      }
    }

  it should "render as 'Complete(...)' with comma-separated values" in
    forAll { (c: Status.Complete[String]) =>
      c.toString shouldBe c.mkString("Complete(", ", ", ")")
    }

  "Complete.apply" should "create a Complete status from a sequence of errors" in {
    val error = mock[AnyRef]
    Status.Complete(Seq(error)) shouldBe Status.Complete(Seq(error))
  }
}
