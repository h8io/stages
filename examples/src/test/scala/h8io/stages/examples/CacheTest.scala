package h8io.stages.examples

import h8io.stages.*
import h8io.stages.examples.Cache.Cached
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
    with StagesCoreTestUtil {
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
          testMappedEvolution(
            cacheEvolution,
            evolution,
            if (status == Status.Success) Cache.Cached[UUID, String, Exception](out, _)
            else Cache[UUID, String, Exception],
            Cache[UUID, String, Exception],
            Cache[UUID, String, Exception]
          )
        case (Yield.None(status, _), Yield.None(cacheStatus, cacheEvolution)) =>
          cacheStatus shouldBe status
          testMappedEvolution(cacheEvolution, evolution, Cache[UUID, String, Exception])
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

  it should "call the alterand.skip() method" in {
    val stage = mock[Stage[UUID, String, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, String, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testMappedEvolution(Cache(stage).skip(), evolution, Cache[UUID, String, Exception])
  }

  "Cached" should "keep output while the status is Success" in
    forAll(Gen.zip(Gen.long, Gen.uuid)) { case (in, out) =>
      val stage = mock[Stage[Long, UUID, Exception]]("underlying stage")
      val evolution = mock[Evolution[Long, UUID, Exception]]("skip")
      (stage.skip _).expects().returns(evolution)
      inside(Cache.Cached(out, stage)(in)) { case Yield.Some(`out`, Status.Success, cachedEvolution) =>
        inSequence {
          val onSuccessStage = mock[Stage[Long, UUID, Exception]]
          (evolution.onSuccess _).expects().returns(onSuccessStage)
          cachedEvolution.onSuccess() shouldBe Cached(out, onSuccessStage)

          val onCompleteStage = mock[Stage[Long, UUID, Exception]]
          (evolution.onComplete _).expects().returns(onCompleteStage)
          cachedEvolution.onComplete() shouldBe Cache(onCompleteStage)

          val onErrorStage = mock[Stage[Long, UUID, Exception]]
          (evolution.onError _).expects().returns(onErrorStage)
          cachedEvolution.onError() shouldBe Cache(onErrorStage)

          (evolution.dispose _).expects()
          noException should be thrownBy cachedEvolution.dispose()
        }
      }
    }

  it should "call the alterand.skip() method" in {
    val out = mock[AnyRef]
    val stage = mock[Stage[UUID, AnyRef, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, AnyRef, Exception]]("evolution")
    val cached = Cached(out, stage)
    inSequence {
      (stage.skip _).expects().returns(evolution)

      val cachedEvolution = cached.skip()

      val onSuccessStage = mock[Stage[UUID, AnyRef, Exception]]
      (evolution.onSuccess _).expects().returns(onSuccessStage)
      cachedEvolution.onSuccess() shouldBe Cached(out, onSuccessStage)

      val onCompleteStage = mock[Stage[UUID, AnyRef, Exception]]
      (evolution.onComplete _).expects().returns(onCompleteStage)
      cachedEvolution.onComplete() shouldBe Cache(onCompleteStage)

      val onErrorStage = mock[Stage[UUID, AnyRef, Exception]]
      (evolution.onError _).expects().returns(onErrorStage)
      cachedEvolution.onError() shouldBe Cache(onErrorStage)

      (evolution.dispose _).expects()
      noException should be thrownBy cachedEvolution.dispose()
    }
  }
}
