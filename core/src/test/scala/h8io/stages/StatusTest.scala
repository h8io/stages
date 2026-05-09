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

  "complete" should "be idempotent" in {
    Status.complete.combine(Status.complete) shouldBe Status.complete
  }

  it should "be overridden by Complete with errors" in
    forAll((c: Status.Complete[String]) => Status.complete.combine(c) shouldBe c)

  "Complete with errors" should "keep the order of causes in composition" in
    forAll { (previous: Status.Complete[String], next: Status.Complete[String]) =>
      previous.combine(next) shouldBe Status.Complete(previous.errors ++ next.errors)
    }

  it should "override complete" in
    forAll((c: Status.Complete[String]) => c.combine(Status.complete) shouldBe c)

  "Complete" should "contain the same elements as the error list" in
    forAll { (c: Status.Complete[Exception]) =>
      c.isEmpty shouldBe c.errors.isEmpty
      c.toList should contain theSameElementsInOrderAs c.errors
      val i = c.iterator
      i.toList should contain theSameElementsInOrderAs c.errors
    }

  it should "render as 'Complete(...)' with comma-separated values" in
    forAll { (c: Status.Complete[String]) =>
      c.toString shouldBe c.errors.mkString("Complete(", ", ", ")")
    }

  "map" should "not change the Success status" in {
    Status.Success.map(mock[String => Long]) shouldBe Status.Success
  }

  it should "not change the complete status" in {
    Status.complete.map(mock[Throwable => String]) shouldBe Status.complete
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

  "Complete.apply" should "create a Complete status from a sequence of errors" in {
    val error = mock[AnyRef]
    Status.Complete(Seq(error)).errors shouldBe Seq(error)
  }

  "error" should "create a Complete from a single error" in {
    val e = mock[AnyRef]
    Status.error(e) shouldBe Status.Complete(Seq(e))
  }

  it should "preserve the order of all error values" in
    forAll(Gen.nonEmptyListOf(Gen.uuid)) { errors =>
      Status.error(errors.head, errors.tail*) shouldBe Status.Complete(errors)
    }
}
