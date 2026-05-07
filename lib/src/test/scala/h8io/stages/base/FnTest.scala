package h8io.stages.base

import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.ZoneId

class FnTest extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks {
  "Fn" should "return the function value as output" in
    forAll { (in: ZoneId, out: Long) =>
      val f = mock[FnPublicMorozov[ZoneId, Long]]
      (f.f _).expects(in).returns(out)
      f(in) shouldBe Yield.Some(`out`, Status.Success, f)
    }
}

// Test seam: widens protected f to public so ScalaMock can set expectations on it.
private trait FnPublicMorozov[-I, +O] extends Fn[I, O] {
  override def f(in: I): O
}
