package h8io.stages.base

import h8io.stages.{Evolution, StagesCoreArbitraries}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant
import java.util.UUID

class SafeStageTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "SafeStage" should "return body result when no exception is thrown" in
    forAll(Gen.zip(Gen.uuid, arbEvolutionToYield[UUID, Long, String].arbitrary)) {
      case (in, evolutionToYield) =>
        val yld = evolutionToYield(mock[Evolution[UUID, Long, String]])
        val stage = mock[SafeStage[UUID, Long, String]]
        (stage.body _).expects(in).returns(yld)
        stage(in) shouldBe yld
    }

  it should "recover from NonFatal exceptions" in
    forAll(Gen.zip(Gen.long, arbEvolutionToYield[Long, Instant, UUID].arbitrary)) {
      case (in, evolutionToYield) =>
        val e = new RuntimeException
        val stage = mock[SafeStage[Long, Instant, UUID]]
        val yld = evolutionToYield(mock[Evolution[Long, Instant, UUID]])
        inSequence {
          (stage.body _).expects(in).throws(e)
          (stage.recover _).expects(in, e).returns(yld)
        }
        stage(in) shouldBe yld
    }

  it should "rethrow fatal exceptions" in {
    val e = new InterruptedException
    val stage = mock[SafeStage[AnyRef, Int, Long]]
    val in = mock[AnyRef]
    (stage.body _).expects(in).throws(e)
    an[InterruptedException] should be thrownBy stage(in)
  }
}
