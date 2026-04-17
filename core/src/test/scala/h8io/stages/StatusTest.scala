package h8io.stages

import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.Duration

class StatusTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "Success" should "be idempotent" in { Status.Success.combine(Status.Success) shouldBe Status.Success }

  it should "call onSuccess() on the Evolution object" in {
    val evolution = mock[Evolution[Long, Instant, Exception]]
    val stage = mock[Stage[Long, Instant, Exception]]
    (evolution.onSuccess _).expects().returns(stage)
    Status.Success(evolution) shouldBe stage
  }

  it should "become Complete when break is called" in { Status.Success.break shouldBe Status.Complete }

  "Complete" should "be idempotent" in { Status.Complete.combine(Status.Complete) shouldBe Status.Complete }

  it should "be overridden by Error" in
    forAll((error: Status.Error[String]) => Status.Complete.combine(error) shouldBe error)

  it should "call onComplete() on the Evolution object" in {
    val evolution = mock[Evolution[Long, Instant, Exception]]
    val stage = mock[Stage[Long, Instant, Exception]]
    (evolution.onComplete _).expects().returns(stage)
    Status.Complete(evolution) shouldBe stage
  }

  it should "not change when break is called" in { Status.Complete.break shouldBe Status.Complete }

  "Error" should "keep the order of causes in composition" in
    forAll { (previous: Status.Error[String], next: Status.Error[String]) =>
      previous.combine(next) shouldBe Status.Error(previous.head, previous.tail ::: next.head :: next.tail)
    }

  it should "override Complete" in
    forAll((error: Status.Error[String]) => error.combine(Status.Complete) shouldBe error)

  it should "call onError() on the Evolution object" in
    forAll { (error: Status.Error[String]) =>
      val evolution = mock[Evolution[Long, Instant, Exception]]
      val stage = mock[Stage[Long, Instant, Exception]]
      (evolution.onError _).expects().returns(stage)
      error(evolution) shouldBe stage
    }

  it should "not change when break is called" in forAll((error: Status.Error[String]) => error.break shouldBe error)

  it should "not be empty" in
    forAll { (error: Status.Error[Exception]) =>
      error.isEmpty shouldBe false
      error.toList should matchPattern { case error.head :: error.tail => }
      val i = error.iterator
      i.next() shouldBe error.head
      i.toList should contain theSameElementsInOrderAs error.tail
    }

  "map" should "not change the Success status" in {
    Status.Success.map(mock[String => Long]) shouldBe Status.Success
  }

  it should "not change the Complete status" in {
    Status.Complete.map(mock[Throwable => String]) shouldBe Status.Complete
  }

  it should "not change the Error status" in
    forAll(Gen.choose(1, 16)) { size =>
      forAll(Gen.zip(Gen.listOfN(size, Gen.uuid), Gen.listOfN(size, Gen.duration))) { case (initial, mapped) =>
        val initalError = Status.Error(initial.head, initial.tail)
        val mappedError = Status.Error(mapped.head, mapped.tail)
        val f = mock[UUID => Duration]
        inSequence(
          for {
            (i, m) <- initial.zip(mapped)
          } (f.apply _).expects(i).returns(m)
        )
        initalError.map(f) shouldBe mappedError
      }
    }

  "Error.apply" should "create an Error status from a single error" in {
    val error = mock[AnyRef]
    Status.Error(error) shouldBe Status.Error(error, Nil)
  }
}
