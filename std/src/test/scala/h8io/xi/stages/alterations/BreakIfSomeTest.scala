package h8io.xi.stages.alterations

import h8io.xi.stages.Stage
import h8io.xi.stages.std.Break
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{ZoneId, ZoneOffset}

class BreakIfSomeTest extends AnyFlatSpec with Matchers with MockFactory {
  "BreakIfSome" should "create a correct stage" in {
    val alterand = mock[Stage[ZoneId, ZoneOffset, Exception]]
    BreakIfSome[ZoneId, ZoneOffset, Exception](alterand) shouldBe alterand ~> Break[ZoneOffset]
  }
}
