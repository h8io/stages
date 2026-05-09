package h8io.stages.operators

import h8io.stages.Stage
import h8io.stages.std.Complete
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{ZoneId, ZoneOffset}

class CompleteIfSomeTest extends AnyFlatSpec with Matchers with MockFactory {
  "CompleteIfSome" should "create a correct stage" in {
    val alterand = mock[Stage[ZoneId, ZoneOffset, Exception]]
    CompleteIfSome[ZoneId, ZoneOffset, Exception](alterand) shouldBe alterand ~> Complete[ZoneOffset]
  }
}
