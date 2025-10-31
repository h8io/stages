package h8io.xi.stages.std

import h8io.xi.stages.{Signal, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.ZoneId

class FnTest extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks {
  "Fn" should "return a function value as an output" in
    forAll { (in: ZoneId, out: Long) =>
      val f = mock[Fn[ZoneId, Long]]
      (f.f _).expects(in).returns(out)
      f(in) shouldBe Yield.Some(`out`, Signal.Success, f)
    }
}
