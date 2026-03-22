package h8io.stages.examples

import h8io.stages.*
import h8io.stages.base.StagesBaseTestUtil
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.util.UUID

class CacheTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesBaseTestUtil {
  "Cache" should "cache output only if the yield is Some and the status is Success" in {
    def test(
        yieldSupplier: StatusAndEvolutionToYield[UUID, String, Exception],
        status: Status[Exception],
        in: UUID): Unit = {
      val stage = mock[Stage[UUID, String, Exception]]("underlying stage")
      val evolution = mock[Evolution[UUID, String, Exception]]("underlying evolution")
      val yld = yieldSupplier(status, evolution)
      val cache = Cache(stage)
      (stage.apply _).expects(in).returns(yld)
      val cacheYield = cache(in)
      inside((yld, cacheYield)) {
        case (Yield.Some(out, status, _), Yield.Some(cacheOut, cacheStatus, cacheEvolution)) =>
          cacheOut shouldBe out
          cacheStatus shouldBe status
          testWrappedEvolution(
            cacheEvolution,
            evolution,
            if (status == Status.Success) Cache.Cached[UUID, String, Exception](out, _)
            else Cache[UUID, String, Exception],
            Cache[UUID, String, Exception],
            Cache[UUID, String, Exception]
          )
        case (Yield.None(status, _), Yield.None(cacheStatus, cacheEvolution)) =>
          cacheStatus shouldBe status
          testWrappedEvolution(cacheEvolution, evolution, Cache[UUID, String, Exception])
      }
    }
    forAll(
      Gen.zip(
        Arbitrary.arbitrary[StatusAndEvolutionToYield[UUID, String, Exception]],
        Arbitrary.arbitrary[Status.Error[Exception]],
        Gen.uuid)) { case (yieldSupplier, error, in) =>
      test(yieldSupplier, Status.Success, in)
      test(yieldSupplier, Status.Complete, in)
      test(yieldSupplier, error, in)
    }
  }

  "Cached" should "keep output while the status is Success" in
    forAll(Gen.zip(Gen.long, Gen.uuid)) { case (in, out) =>
      val stage = mock[Stage[Long, UUID, Exception]]("underlying stage")
      val cached = Cache.Cached(out, stage)
      inside(cached(in)) { case Yield.Some(`out`, Status.Success, evolution) =>
        evolution.onSuccess() shouldBe cached
        evolution.onComplete() shouldBe Cache(stage)
        evolution.onError() shouldBe Cache(stage)
      }
    }
}
