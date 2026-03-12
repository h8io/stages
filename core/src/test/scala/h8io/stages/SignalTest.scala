package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant

class SignalTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "Success" should "be idempotent" in { Signal.Success ++ Signal.Success shouldBe Signal.Success }

  it should "call onSuccess() on the OnDone object" in {
    val onDone = mock[OnDone[Long, Instant, Exception]]
    val stage = mock[Stage[Long, Instant, Exception]]
    (onDone.onSuccess _).expects().returns(stage)
    Signal.Success(onDone) shouldBe stage
  }

  it should "become Complete when break is called" in { Signal.Success.break shouldBe Signal.Complete }

  "Complete" should "be idempotent" in { Signal.Complete ++ Signal.Complete shouldBe Signal.Complete }

  it should "be overridden by Error" in
    forAll((error: Signal.Error[String]) => Signal.Complete ++ error shouldBe error)

  it should "call onComplete() on the OnDone object" in {
    val onDone = mock[OnDone[Long, Instant, Exception]]
    val stage = mock[Stage[Long, Instant, Exception]]
    (onDone.onComplete _).expects().returns(stage)
    Signal.Complete(onDone) shouldBe stage
  }

  it should "not change when break is called" in { Signal.Complete.break shouldBe Signal.Complete }

  "Error" should "keep the order of causes in composition" in
    forAll { (previous: Signal.Error[String], next: Signal.Error[String]) =>
      previous ++ next shouldBe Signal.Error(previous.head, previous.tail ::: next.head :: next.tail)
    }

  it should "override Complete" in forAll((error: Signal.Error[String]) => error ++ Signal.Complete shouldBe error)

  it should "call onError() on the OnDone object" in
    forAll { (error: Signal.Error[String]) =>
      val onDone = mock[OnDone[Long, Instant, Exception]]
      val stage = mock[Stage[Long, Instant, Exception]]
      (onDone.onError _).expects().returns(stage)
      error(onDone) shouldBe stage
    }

  it should "not change when break is called" in forAll((error: Signal.Error[String]) => error.break shouldBe error)

  it should "not be empty" in
    forAll { (error: Signal.Error[Exception]) =>
      error.isEmpty shouldBe false
      error.toList should matchPattern { case error.head :: error.tail => }
      val i = error.iterator
      i.next() shouldBe error.head
      i.toList should contain theSameElementsInOrderAs error.tail
    }
}
