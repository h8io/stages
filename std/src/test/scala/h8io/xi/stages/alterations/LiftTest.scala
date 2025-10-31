package h8io.xi.stages.alterations

import h8io.xi.stages.{OnDone, Stage, StagesCoreArbitraries, StagesCoreTestUtil, Yield}
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
    forAll { (in: Int, yieldSupplier: OnDoneToYieldSome[Int, String, UUID]) =>
      val stage = mock[Stage[Int, String, UUID]]
      val onDone = mock[OnDone[Int, String, UUID]]
      val yld = yieldSupplier(onDone)
      (stage.apply _).expects(in).returns(yld)
      inside(Lift(stage)(in)) { case Yield.Some(Some(yld.out), yld.signal, wrappedOnDone) =>
        testWrappedOnDone(wrappedOnDone, onDone, Lift[Int, String, UUID])
      }
    }

  it should "transform Yield.None to Yield.Some" in
    forAll { (in: Int, yieldSupplier: OnDoneToYieldNone[Long, Instant, String]) =>
      val stage = mock[Stage[Long, Instant, String]]
      val onDone = mock[OnDone[Long, Instant, String]]
      val yld = yieldSupplier(onDone)
      (stage.apply _).expects(in).returns(yld)
      inside(Lift(stage)(in)) { case Yield.Some(None, yld.signal, wrappedOnDone) =>
        testWrappedOnDone(wrappedOnDone, onDone, Lift[Long, Instant, String])
      }
    }
}
