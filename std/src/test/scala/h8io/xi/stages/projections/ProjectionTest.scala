package h8io.xi.stages.projections

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProjectionTest extends AnyFlatSpec with Matchers with MockFactory {
  trait Projectable[+L, +R]

  "LeftProjection" should "be casted to a specific type" in {
    val leftUntyped = mock[LeftProjection[Projectable]]
    val left: Projection[Projectable[Int, ?], Int] = leftUntyped[Int]
    left should be theSameInstanceAs leftUntyped
  }

  "RightProjection" should "be casted to a specific type" in {
    val rightUntyped = mock[RightProjection[Projectable]]
    val right: Projection[Projectable[?, String], String] = rightUntyped[String]
    right should be theSameInstanceAs rightUntyped
  }
}
