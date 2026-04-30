package h8io.stages.operators

import h8io.stages.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant
import java.util.UUID

class LiftTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "Lift" should "transform output of Yield.Some to Some" in
    forAll { (in: Int, yieldSupplier: EvolutionToYieldSome[Int, String, UUID]) =>
      val stage = mock[Stage[Int, String, UUID]]
      val evolution = mock[Evolution[Int, String, UUID]]
      val yld = yieldSupplier(evolution)
      (stage.apply _).expects(in).returns(yld)
      inside(Lift(stage)(in)) { case Yield.Some(Some(yld.out), yld.`status`, wrappedEvolution) =>
        testMappedEvolution(wrappedEvolution, evolution, Lift[Int, String, UUID])
      }
    }

  it should "transform Yield.None to Yield.Some" in
    forAll { (in: Long, yieldSupplier: EvolutionToYieldNone[Long, Instant, String]) =>
      val stage = mock[Stage[Long, Instant, String]]
      val evolution = mock[Evolution[Long, Instant, String]]
      val yld = yieldSupplier(evolution)
      (stage.apply _).expects(in).returns(yld)
      inside(Lift(stage)(in)) { case Yield.Some(None, yld.`status`, wrappedEvolution) =>
        testMappedEvolution(wrappedEvolution, evolution, Lift[Long, Instant, String])
      }
    }

  it should "call the alterand.skip() method" in {
    val stage = mock[Stage[UUID, Long, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, Long, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testMappedEvolution(Lift(stage).skip(), evolution, Lift[UUID, Long, Exception])
  }
}
