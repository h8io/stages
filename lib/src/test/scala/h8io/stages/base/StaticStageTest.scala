package h8io.stages.base

import h8io.stages.{StagesCoreArbitraries, Yield}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.util.UUID

class StaticStageTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "StaticStage" should "return the output value, status and self as output" in
    forAll(Gen.zip(Gen.long, Gen.option(Gen.uuid), arbStatus[String].arbitrary)) {
      case (in: Long, out: Option[UUID], status) =>
        val stage = mock[StaticStagePublicMorozov[Long, UUID, String]]
        val (staticOut, stageOut) = out match {
          case Some(out) => (StaticYield.Some(out, status), Yield.Some(out, status, stage))
          case None => (StaticYield.None(status), Yield.None(status, stage))
        }
        (stage.process _).expects(in).returns(staticOut)
        stage(in) shouldBe stageOut
    }
}

// Test seam: widens protected `process` to public so ScalaMock can set expectations on it.
private trait StaticStagePublicMorozov[-I, +O, +E] extends StaticStage[I, O, E] {
  override def process(in: I): StaticYield[O, E]
}
