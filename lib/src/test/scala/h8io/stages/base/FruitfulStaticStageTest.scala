package h8io.stages.base

import h8io.stages.{StagesCoreArbitraries, Status, Yield}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.util.UUID

class FruitfulStaticStageTest
    extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks with StagesCoreArbitraries {
  "FruitfulStaticStage" should "return the output value, status and self as evolution" in
    forAll(Gen.zip(Gen.long, Gen.uuid, arbStatus[String].arbitrary)) { case (in: Long, out: UUID, status) =>
      val stage = mock[FruitfulStaticStagePublicMorozov[Long, UUID, String]]
      (stage.process _).expects(in).returns((out, status))
      stage(in) shouldBe Yield.Some(out, status, stage)
    }
}

// Test seam: widens protected `produce` to public so ScalaMock can set expectations on it; `apply` and `process` are
// final, so ScalaMock keeps their real implementations under test.
private trait FruitfulStaticStagePublicMorozov[-I, +O, +E] extends FruitfulStaticStage[I, O, E] {
  override def process(in: I): (O, Status[E])
}
