package h8io.xi.stages.alterations

import h8io.xi.stages.{OnDone, Stage, StagesCoreArbitraries, StagesCoreTestUtil, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Instant, LocalDate}

class BreakIfNoneTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "BreakIfNone" should "return Yield.Some if the alterand result is Yield.Some" in
    forAll { (in: Long, yieldSupplier: OnDoneToYieldSome[Long, Instant, String]) =>
      val alterand = mock[Stage[Long, Instant, String]]
      val onDone = mock[OnDone[Long, Instant, String]]
      val yld = yieldSupplier(onDone)
      (alterand.apply _).expects(in).returns(yld)
      inside(BreakIfNone(alterand)(in)) { case Yield.Some(yld.out, yld.signal, binOnDone) =>
        testWrappedOnDone(binOnDone, onDone, BreakIfNone[Long, Instant, String])
      }
    }

  it should "return Yield.None with breaking signal if the alterand result is Yield.None" in
    forAll { (in: String, yieldSupplier: OnDoneToYieldNone[String, LocalDate, Long]) =>
      val alterand = mock[Stage[String, LocalDate, Long]]
      val onDone = mock[OnDone[String, LocalDate, Long]]
      val yld = yieldSupplier(onDone)
      (alterand.apply _).expects(in).returns(yld)
      inside(BreakIfNone(alterand)(in)) { case Yield.None(signal, binOnDone) =>
        signal shouldBe yld.signal.break
        testWrappedOnDone(binOnDone, onDone, BreakIfNone[String, LocalDate, Long])
      }
    }
}
