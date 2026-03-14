package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant

class StatusTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "Success" should "be idempotent" in { Status.Success ++ Status.Success shouldBe Status.Success }

  it should "call onSuccess() on the OnDone object" in {
    val onDone = mock[OnDone[Long, Instant, Exception]]
    val stage = mock[Stage[Long, Instant, Exception]]
    (onDone.onSuccess _).expects().returns(stage)
    Status.Success(onDone) shouldBe stage
  }

  it should "become Complete when break is called" in { Status.Success.break shouldBe Status.Complete }

  "Complete" should "be idempotent" in { Status.Complete ++ Status.Complete shouldBe Status.Complete }

  it should "be overridden by Error" in
    forAll((error: Status.Error[String]) => Status.Complete ++ error shouldBe error)

  it should "call onComplete() on the OnDone object" in {
    val onDone = mock[OnDone[Long, Instant, Exception]]
    val stage = mock[Stage[Long, Instant, Exception]]
    (onDone.onComplete _).expects().returns(stage)
    Status.Complete(onDone) shouldBe stage
  }

  it should "not change when break is called" in { Status.Complete.break shouldBe Status.Complete }

  "Error" should "keep the order of causes in composition" in
    forAll { (previous: Status.Error[String], next: Status.Error[String]) =>
      previous ++ next shouldBe Status.Error(previous.head, previous.tail ::: next.head :: next.tail)
    }

  it should "override Complete" in forAll((error: Status.Error[String]) => error ++ Status.Complete shouldBe error)

  it should "call onError() on the OnDone object" in
    forAll { (error: Status.Error[String]) =>
      val onDone = mock[OnDone[Long, Instant, Exception]]
      val stage = mock[Stage[Long, Instant, Exception]]
      (onDone.onError _).expects().returns(stage)
      error(onDone) shouldBe stage
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
}
