package h8io.stages.base

import h8io.stages.{Evolution, Stage, StagesCoreArbitraries}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant
import java.util.UUID

class StageOpsTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "toEvolution" should "return ConstEvolution" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]("stage")
    stage.toEvolution shouldBe ConstEvolution(stage)
    val dispose = mock[() => Unit]("dispose")
    stage.toEvolution(dispose) shouldBe ConstEvolution(stage, dispose)
  }

  "execute" should "run evolution and return Outcome.Some" in
    forAll { (in: Long, yieldSupplier: EvolutionToYieldSome[Long, String, UUID]) =>
      val stage = mock[Stage[Long, String, UUID]]
      val evolution = mock[Evolution[Long, String, UUID]]
      val yld = yieldSupplier(evolution)
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        (evolution.dispose _).expects()
      }
      stage.execute(in) shouldBe Outcome.Some(yld.out, yld.status, None)
    }

  it should "run evolution and return Outcome.None" in
    forAll { (in: Instant, yieldSupplier: EvolutionToYieldNone[Instant, Boolean, Long]) =>
      val stage = mock[Stage[Instant, Boolean, Long]]
      val evolution = mock[Evolution[Instant, Boolean, Long]]
      val yld = yieldSupplier(evolution)
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        (evolution.dispose _).expects()
      }
      stage.execute(in) shouldBe Outcome.None(yld.status, None)
    }

  it should "not fail on dispose throw and return Outcome.Some" in
    forAll { (in: Long, yieldSupplier: EvolutionToYieldSome[Long, String, UUID]) =>
      val stage = mock[Stage[Long, String, UUID]]
      val evolution = mock[Evolution[Long, String, UUID]]
      val yld = yieldSupplier(evolution)
      val disposeFailure = new Exception("dispose failed")
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        (evolution.dispose _).expects().throws(disposeFailure)
      }
      stage.execute(in) shouldBe Outcome.Some(yld.out, yld.status, Some(disposeFailure))
    }

  it should "not fail on dispose throw and return Outcome.None" in
    forAll { (in: Instant, yieldSupplier: EvolutionToYieldNone[Instant, Boolean, Long]) =>
      val stage = mock[Stage[Instant, Boolean, Long]]
      val evolution = mock[Evolution[Instant, Boolean, Long]]
      val yld = yieldSupplier(evolution)
      val disposeFailure = new Exception("dispose failed")
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        (evolution.dispose _).expects().throws(disposeFailure)
      }
      stage.execute(in) shouldBe Outcome.None(yld.status, Some(disposeFailure))
    }
}
