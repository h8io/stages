package h8io.stages

import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.*
import java.util.UUID
import scala.annotation.nowarn

class YieldTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "compose" should "compose Some and Some correctly" in
    forAll {
      (upstreamYieldSupplier: EvolutionToYieldSome[Long, Instant, String],
          downstreamYieldSupplier: EvolutionToYieldSome[Instant, String, String]) =>
        val upstreamEvolution = mock[Evolution[Long, Instant, String]]
        val upstreamYield = upstreamYieldSupplier(upstreamEvolution)
        val downstreamEvolution = mock[Evolution[Instant, String, String]]
        val downstreamYield = downstreamYieldSupplier(downstreamEvolution)
        inside(upstreamYield.compose(downstreamYield)) {
          case Yield.Some(downstreamYield.out, status, evolution) =>
            status shouldBe upstreamYield.status.combine(downstreamYield.status)
            testEvolutionComposition[Long, Instant, Instant, String, Long, String, String](
              evolution, upstreamEvolution, downstreamEvolution, _ ~> _)
        }
    }

  it should "compose Some and None correctly" in
    forAll {
      (upstreamYieldSupplier: EvolutionToYieldSome[Long, Instant, String],
          downstreamYieldSupplier: EvolutionToYieldNone[Instant, String, String]) =>
        val upstreamEvolution = mock[Evolution[Long, Instant, String]]
        val upstreamYield = upstreamYieldSupplier(upstreamEvolution)
        val downstreamEvolution = mock[Evolution[Instant, String, String]]
        val downstreamYield = downstreamYieldSupplier(downstreamEvolution)
        inside(upstreamYield.compose(downstreamYield)) {
          case Yield.None(status, evolution) =>
            status shouldBe upstreamYield.status.combine(downstreamYield.status)
            testEvolutionComposition[Long, Instant, Instant, String, Long, String, String](
              evolution, upstreamEvolution, downstreamEvolution, _ ~> _)
        }
    }

  it should "compose None and Evolution correctly" in
    forAll { (upstreamYieldSupplier: EvolutionToYieldNone[Long, Instant, String]) =>
      val upstreamEvolution = mock[Evolution[Long, Instant, String]]("upstream evolution")
      val downstreamEvolution = mock[Evolution[Instant, String, String]]("downstream evolution")
      val upstreamYield = upstreamYieldSupplier(upstreamEvolution)
      inside(upstreamYield.compose(downstreamEvolution)) {
        case Yield.None(upstreamYield.`status`, evolution) =>
          testEvolutionComposition[Long, Instant, Instant, String, Long, String, String](
            evolution, upstreamEvolution, downstreamEvolution, _ ~> _)
      }
    }

  "map" should "transform Yield.Some correctly" in
    forAll(Gen.zip(Gen.uuid, Gen.long, arbStatus[Long].arbitrary, arbStatus[String].arbitrary)) {
      case (initialOut, mappedOut, initialStatus, mappedStatus) =>
        val mapOut = mock[UUID => Long]
        (mapOut.apply _).expects(initialOut).returns(mappedOut)
        val mapStatus = mock[Status[Long] => Status[String]]
        (mapStatus.apply _).expects(initialStatus).returns(mappedStatus)
        val initialEvolution = mock[Evolution[Long, UUID, Long]]("initial Evolution")
        val mappedEvolution = mock[Evolution[String, Long, String]]("mapped Evolution")
        val mapEvolution =
          mock[Evolution[Long, UUID, Long] => Evolution[String, Long, String]]("mapEvolution")
        (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
        Yield.Some(initialOut, initialStatus, initialEvolution).map(mapOut, mapStatus, mapEvolution) shouldBe
          Yield.Some(mappedOut, mappedStatus, mappedEvolution)
    }

  it should "transform Yield.None correctly without mapping output" in
    forAll(Gen.zip(arbStatus[Throwable].arbitrary, arbStatus[Long].arbitrary)) {
      case (initialStatus, mappedStatus) =>
        val mapOut = mock[LocalDateTime => UUID]
        val mapStatus = mock[Status[Throwable] => Status[Long]]
        (mapStatus.apply _).expects(initialStatus).returns(mappedStatus)
        val initialEvolution = mock[Evolution[Instant, LocalDateTime, Throwable]]("initial Evolution")
        val mappedEvolution = mock[Evolution[String, UUID, Long]]("mapped Evolution")
        val mapEvolution =
          mock[Evolution[Instant, LocalDateTime, Throwable] => Evolution[String, UUID, Long]]("mapEvolution")
        (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
        Yield.None(initialStatus, initialEvolution).map(mapOut, mapStatus, mapEvolution) shouldBe
          Yield.None(mappedStatus, mappedEvolution)
    }

  "evolve" should "call evolution according to status" in
    forAll { (yldSupplier: EvolutionToYield[Instant, LocalDateTime, Long]) =>
      val evolution = mock[Evolution[Instant, LocalDateTime, Long]]("evolution")
      val yld = yldSupplier(evolution)
      val stage = mock[Stage[Instant, LocalDateTime, Long]]
      (evolution.evolve _).expects(yld.status).returns(stage)
      yld.evolve() shouldBe stage
    }

  "outOption" should "return None for None Yield" in {
    Yield.None[Instant, LocalDateTime, Long](Status.Success, mock[Evolution[Instant, LocalDateTime, Long]]("evolution"))
      .outOption shouldBe None
  }

  it should "return Some(out) for Some Yield" in {
    val out = mock[AnyRef]
    Yield.Some(out, Status.Success, mock[Evolution[Instant, LocalDateTime, Long]]("evolution")).outOption shouldBe
      Some(out)
  }

  "apply" should "create Yield.Some from a Some output and a Success status" in {
    val out = mock[AnyRef]("out")
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    Yield(Some(out), Status.Success, evolution) shouldBe Yield.Some(out, Status.Success, evolution)
  }

  it should "create Yield.Some from a Some output and a Complete status" in {
    val out = mock[AnyRef]("out")
    val status = Status.Complete(mock[Seq[AnyRef]]("errors"): @nowarn("msg=Override className instead"))
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    Yield(Some(out), status, evolution) shouldBe Yield.Some(out, status, evolution)
  }

  it should "create Yield.None from a None output and a Success status" in {
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    Yield(None, Status.Success, evolution) shouldBe Yield.None(Status.Success, evolution)
  }

  it should "create Yield.None from a None output and a Complete status" in {
    val status = Status.Complete(mock[Seq[AnyRef]]("errors"): @nowarn("msg=Override className instead"))
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    Yield(None, status, evolution) shouldBe Yield.None(status, evolution)
  }

  "unapply" should "extract a Some output and a Success status from Yield.Some" in {
    val out = mock[AnyRef]("out")
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    val yld = Yield.Some(out, Status.Success, evolution)
    val Yield(extractedOut, extractedStatus, extractedEvolution) = yld
    extractedOut shouldBe Some(out)
    extractedStatus shouldBe Status.Success
    extractedEvolution shouldBe evolution
    Yield(extractedOut, extractedStatus, extractedEvolution) shouldBe yld
  }

  it should "extract a Some output and a Complete status from Yield.Some" in {
    val out = mock[AnyRef]("out")
    val status = Status.Complete(mock[Seq[AnyRef]]("errors"): @nowarn("msg=Override className instead"))
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    val yld = Yield.Some(out, status, evolution)
    val Yield(extractedOut, extractedStatus, extractedEvolution) = yld
    extractedOut shouldBe Some(out)
    extractedStatus shouldBe status
    extractedEvolution shouldBe evolution
    Yield(extractedOut, extractedStatus, extractedEvolution) shouldBe yld
  }

  it should "extract a None output and a Success status from Yield.None" in {
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    val yld = Yield.None(Status.Success, evolution)
    val Yield(extractedOut, extractedStatus, extractedEvolution) = yld
    extractedOut shouldBe None
    extractedStatus shouldBe Status.Success
    extractedEvolution shouldBe evolution
    Yield(extractedOut, extractedStatus, extractedEvolution) shouldBe yld
  }

  it should "extract a None output and a Complete status from Yield.None" in {
    val status = Status.Complete(mock[Seq[AnyRef]]("errors"): @nowarn("msg=Override className instead"))
    val evolution = mock[Evolution[Any, Any, Any]]("evolution")
    val yld = Yield.None(status, evolution)
    val Yield(extractedOut, extractedStatus, extractedEvolution) = yld
    extractedOut shouldBe None
    extractedStatus shouldBe status
    extractedEvolution shouldBe evolution
    Yield(extractedOut, extractedStatus, extractedEvolution) shouldBe yld
  }
}
