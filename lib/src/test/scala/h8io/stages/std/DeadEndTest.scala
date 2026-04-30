package h8io.stages.std

import h8io.stages.base.StageOps
import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DeadEndTest extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks {
  "DeadEnd" should "return None for any argument" in {
    val dispose = mock[() => Unit]
    val stage = DeadEnd(dispose)
    stage(mock[AnyRef]) shouldBe Yield.None(Status.complete, stage.toEvolution(dispose))
  }
}
