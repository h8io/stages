package h8io.stages.operators

import h8io.stages.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Instant, LocalDate}
import java.util.UUID

class BreakIfNoneTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "BreakIfNone" should "return Yield.Some if the alterand result is Yield.Some" in
    forAll { (in: Long, yieldSupplier: EvolutionToYieldSome[Long, Instant, String]) =>
      val alterand = mock[Stage[Long, Instant, String]]
      val evolution = mock[Evolution[Long, Instant, String]]
      val yld = yieldSupplier(evolution)
      (alterand.apply _).expects(in).returns(yld)
      inside(BreakIfNone(alterand)(in)) { case Yield.Some(yld.out, yld.status, binEvolution) =>
        testMappedEvolution(binEvolution, evolution, BreakIfNone[Long, Instant, String])
      }
    }

  it should "return Yield.None with breaking status if the alterand result is Yield.None" in
    forAll { (in: String, yieldSupplier: EvolutionToYieldNone[String, LocalDate, Long]) =>
      val alterand = mock[Stage[String, LocalDate, Long]]
      val evolution = mock[Evolution[String, LocalDate, Long]]
      val yld = yieldSupplier(evolution)
      (alterand.apply _).expects(in).returns(yld)
      val expectedStatus = if (yld.status == Status.Success) Status.complete else yld.status
      inside(BreakIfNone(alterand)(in)) { case Yield.None(`expectedStatus`, binEvolution) =>
        testMappedEvolution(binEvolution, evolution, BreakIfNone[String, LocalDate, Long])
      }
    }

  it should "call the alterand.skip() method" in {
    val stage = mock[Stage[Long, UUID, Exception]]("alterand")
    val evolution = mock[Evolution[Long, UUID, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testMappedEvolution(BreakIfNone(stage).skip(), evolution, BreakIfNone[Long, UUID, Exception])
  }
}
