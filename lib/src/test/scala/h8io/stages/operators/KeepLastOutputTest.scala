package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.StagesBaseTestUtil
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
    with StagesCoreTestUtil
    with StagesBaseTestUtil {
  "Initial stage" should "be None" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    KeepLastOutput(stage) shouldBe KeepLastOutput.None(stage)
  }

  "None" should "stay None if alterand returns Yield.None" in
    forAll { (in: UUID, status: Status[Exception]) =>
      val stage = mock[Stage[UUID, Instant, Exception]]
      val evolution = mock[Evolution[UUID, Instant, Exception]]
      (stage.apply _).expects(in).returns(Yield.None(status, evolution))
      inside(KeepLastOutput.None(stage)(in)) { case Yield.None(`status`, kloEvolution) =>
        testWrappedEvolution(kloEvolution, evolution, KeepLastOutput.None[UUID, Instant, Exception])
      }
    }

  it should "become Some if alterand returns Yield.Some" in
    forAll { (in: Long, out: String, status: Status[Exception]) =>
      val stage = mock[Stage[Long, String, Exception]]
      val evolution = mock[Evolution[Long, String, Exception]]
      (stage.apply _).expects(in).returns(Yield.Some(out, status, evolution))
      inside(KeepLastOutput.None(stage)(in)) { case Yield.Some(`out`, `status`, kloEvolution) =>
        testWrappedEvolution(kloEvolution, evolution, KeepLastOutput.Some[Long, String, Exception](out, _))
      }
    }

  it should "call the alterand.skip() method" in {
    val stage = mock[Stage[UUID, String, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, String, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testAlteredEvolution(KeepLastOutput.None(stage).skip(), evolution, KeepLastOutput.None[UUID, String, Exception])
  }

  "Some" should "keep the old output if alterand returns Yield.None" in
    forAll { (in: ZonedDateTime, out: ZoneId, status: Status[Exception]) =>
      val stage = mock[Stage[ZonedDateTime, ZoneId, Exception]]
      val evolution = mock[Evolution[ZonedDateTime, ZoneId, Exception]]
      (stage.apply _).expects(in).returns(Yield.None(status, evolution))
      inside(KeepLastOutput.Some(out, stage)(in)) { case Yield.Some(`out`, `status`, kloEvolution) =>
        testWrappedEvolution(kloEvolution, evolution, KeepLastOutput.Some[ZonedDateTime, ZoneId, Exception](out, _))
      }
    }

  it should "memoize the new output if alterand returns Yield.Some" in
    forAll { (in: Array[Int], out: BigInt, newOut: BigInt, status: Status[Exception]) =>
      val stage = mock[Stage[Array[Int], BigInt, Exception]]
      val evolution = mock[Evolution[Array[Int], BigInt, Exception]]
      (stage.apply _).expects(in).returns(Yield.Some(newOut, status, evolution))
      inside(KeepLastOutput.Some(out, stage)(in)) { case Yield.Some(`newOut`, `status`, kloEvolution) =>
        testWrappedEvolution(kloEvolution, evolution, KeepLastOutput.Some[Array[Int], BigInt, Exception](newOut, _))
      }
    }

  it should "call the alterand.skip() method" in {
    val out = mock[AnyRef]
    val stage = mock[Stage[UUID, AnyRef, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, AnyRef, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testAlteredEvolution(
      KeepLastOutput.Some(out, stage).skip(), evolution, KeepLastOutput.Some[UUID, AnyRef, Exception](out, _))
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
