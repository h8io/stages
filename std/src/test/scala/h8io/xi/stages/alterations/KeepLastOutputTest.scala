package h8io.xi.stages.alterations

import h8io.xi.stages.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

class KeepLastOutputTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "Initial stage" should "should be None" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    KeepLastOutput(stage) shouldBe KeepLastOutput.None(stage)
  }

  "None" should "stay None if alterand returns Yield.None" in
    forAll { (in: UUID, signal: Signal[Exception]) =>
      val stage = mock[Stage[UUID, Instant, Exception]]
      val onDone = mock[OnDone[UUID, Instant, Exception]]
      (stage.apply _).expects(in).returns(Yield.None(signal, onDone))
      inside(KeepLastOutput.None(stage)(in)) { case Yield.None(`signal`, kloOnDone) =>
        testWrappedOnDone(kloOnDone, onDone, KeepLastOutput.None[UUID, Instant, Exception])
      }
    }

  it should "become Some if alterand returns Yield.Some" in
    forAll { (in: Long, out: String, signal: Signal[Exception]) =>
      val stage = mock[Stage[Long, String, Exception]]
      val onDone = mock[OnDone[Long, String, Exception]]
      (stage.apply _).expects(in).returns(Yield.Some(out, signal, onDone))
      inside(KeepLastOutput.None(stage)(in)) { case Yield.Some(`out`, `signal`, kloOnDone) =>
        testWrappedOnDone(kloOnDone, onDone, KeepLastOutput.Some[Long, String, Exception](out, _))
      }
    }

  "Some" should "keep the old output if alterand returns Yield.None" in
    forAll { (in: ZonedDateTime, out: ZoneId, signal: Signal[Exception]) =>
      val stage = mock[Stage[ZonedDateTime, ZoneId, Exception]]
      val onDone = mock[OnDone[ZonedDateTime, ZoneId, Exception]]
      (stage.apply _).expects(in).returns(Yield.None(signal, onDone))
      inside(KeepLastOutput.Some(out, stage)(in)) { case Yield.Some(`out`, `signal`, kloOnDone) =>
        testWrappedOnDone(kloOnDone, onDone, KeepLastOutput.Some[ZonedDateTime, ZoneId, Exception](out, _))
      }
    }

  it should "memoize the new output if alterand returns Yield.Some" in
    forAll { (in: Array[Int], out: BigInt, newOut: BigInt, signal: Signal[Exception]) =>
      val stage = mock[Stage[Array[Int], BigInt, Exception]]
      val onDone = mock[OnDone[Array[Int], BigInt, Exception]]
      (stage.apply _).expects(in).returns(Yield.Some(newOut, signal, onDone))
      inside(KeepLastOutput.Some(out, stage)(in)) { case Yield.Some(`newOut`, `signal`, kloOnDone) =>
        testWrappedOnDone(kloOnDone, onDone, KeepLastOutput.Some[Array[Int], BigInt, Exception](newOut, _))
      }
    }

  "dispose" should "call alterand's dispose for None" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    (stage.dispose _).expects()
    noException should be thrownBy KeepLastOutput.None(stage).dispose()
  }

  it should "call alterand's dispose for Some" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    (stage.dispose _).expects()
    noException should be thrownBy KeepLastOutput.Some(mock[AnyRef], stage).dispose()
  }
}
