package h8io.xi.stages.alterations

import h8io.xi.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IdentityTest extends AnyFlatSpec with Matchers with MockFactory {
  "Identity" should "transform alterand to itself" in {
    val alterand = mock[Stage[Array[Int], String, Exception]]
    Identity[Array[Int], String, Exception](alterand) should be theSameInstanceAs alterand
  }
}
