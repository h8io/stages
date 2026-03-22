package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.sql.Timestamp
import java.time.*
import java.util.UUID

class StageTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "dispose" should "do nothing" in {
    noException should be thrownBy new Stage[Instant, Timestamp, String] {
      def apply(in: Instant): Yield[Instant, Timestamp, String] = throw new NotImplementedError
    }.dispose()
  }

  "outcome" should "run evolution and return Outcome.Some" in
    forAll { (in: Long, yieldSupplier: EvolutionToYieldSome[Long, String, UUID]) =>
      val stage = mock[Stage[Long, String, UUID]]
      val evolution = mock[Evolution[Long, String, UUID]]
      val yld = yieldSupplier(evolution)
      val evolved = mock[Stage[Long, String, UUID]]
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        evolutionMock(evolution, yld.status, evolved)
        (evolved.dispose _).expects()
      }
      stage.execute(in) shouldBe Outcome.Some(yld.out, yld.status)
    }

  it should "run evolution and return Outcome.None" in
    forAll { (in: Instant, yieldSupplier: EvolutionToYieldNone[Instant, Boolean, Long]) =>
      val stage = mock[Stage[Instant, Boolean, Long]]
      val evolution = mock[Evolution[Instant, Boolean, Long]]
      val yld = yieldSupplier(evolution)
      val evolved = mock[Stage[Instant, Boolean, Long]]
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        evolutionMock(evolution, yld.status, evolved)
        (evolved.dispose _).expects()
      }
      stage.execute(in) shouldBe Outcome.None(yld.status)
    }

  "~>" should "produce a Stage.AndThen with a stage argument" in {
    val previous = mock[Stage[String, Long, Nothing]]
    val next = mock[Stage[Long, Timestamp, String]]
    previous ~> next shouldBe Stage.AndThen(previous, next)
  }

  it should "produce an alteration when given an alteration argument" in {
    val stage = mock[Stage[Int, Long, UUID]]
    val alteration = mock[Alteration[Stage[ZoneId, ZonedDateTime, String], Stage[Long, String, Nothing]]]
    val in = mock[Stage[ZoneId, ZonedDateTime, String]]
    val out = mock[Stage[Long, String, Nothing]]
    (alteration.apply _).expects(in).returns(out)
    val result: Stage[Int, String, UUID] = (stage ~> alteration)(in)
    result shouldBe stage ~> out
    (alteration.apply _).expects(in).returns(out)
    stage ~> alteration <| in shouldBe stage ~> out
  }

  "<~" should "produce a Stage.AndThen" in {
    val previous = mock[Stage[Instant, Int, String]]
    val next = mock[Stage[Int, String, Nothing]]
    next <~ previous shouldBe Stage.AndThen(previous, next)
  }

  "|>" should "apply an alteration to a stage" in {
    val stage = mock[Stage[ZoneOffset, OffsetDateTime, Exception]]
    val alteration = mock[Alteration[Stage[ZoneOffset, OffsetDateTime, Exception], Stage[UUID, Instant, Long]]]
    val out = mock[Stage[UUID, Instant, Long]]
    (alteration.apply _).expects(stage).returns(out)
    stage |> alteration shouldBe out
  }

  "alteration" should "return leftAlteration" in {
    val left = mock[Stage[Int, Long, Nothing]]
    val right = mock[Stage[Long, Duration, Exception]]
    left.alteration(right) shouldBe left ~> right
  }

  "leftAlteration" should "produce a composition with the predefined left operand on the left" in {
    val left = mock[Stage[Int, Long, Nothing]]
    val right = mock[Stage[Long, Duration, Exception]]
    val alteration: Alteration[Stage[Long, Duration, Exception], Stage[Int, Duration, Exception]] = left.leftAlteration
    alteration(right) shouldBe left ~> right
  }

  "rightAlteration" should "produce a composition with the predefined right operand on the right" in {
    val left = mock[Stage[Int, Long, Nothing]]
    val right = mock[Stage[Long, Duration, Exception]]
    val alteration: Alteration[Stage[Int, Long, Nothing], Stage[Int, Duration, Exception]] = right.rightAlteration
    alteration(left) shouldBe left ~> right
  }

  "AndThen" should "call stages sequentially and return the correct Yield for Some ~> Some" in
    forAll {
      (previousStatus: Status[String], nextStatus: Status[String], in: Int, previousOut: String, nextOut: Long) =>
        val previousStage = mock[Stage[Int, String, String]]
        val previousEvolution = mock[Evolution[Int, String, String]]
        val nextStage = mock[Stage[String, Long, String]]
        val nextEvolution = mock[Evolution[String, Long, String]]
        inSequence {
          (previousStage.apply _).expects(in).returns(Yield.Some(previousOut, previousStatus, previousEvolution))
          (nextStage.apply _).expects(previousOut).returns(Yield.Some(nextOut, nextStatus, nextEvolution))
        }
        inside(Stage.AndThen(previousStage, nextStage)(in)) { case Yield.Some(`nextOut`, status, evolution) =>
          status shouldBe previousStatus ++ nextStatus
          val updatedPreviousStage = mock[Stage[Int, String, String]]
          val updatedNextStage = mock[Stage[String, Long, Nothing]]
          inSequence {
            armEvolution(nextEvolution, status, updatedNextStage)
            armEvolution(previousEvolution, status, updatedPreviousStage)
          }
          status(evolution) shouldBe Stage.AndThen(updatedPreviousStage, updatedNextStage)
        }
    }

  it should "call stages sequentially and return the correct Yield for Some ~> None" in
    forAll { (previousStatus: Status[String], nextStatus: Status[String], in: Int, out: String) =>
      val previousStage = mock[Stage[Int, String, String]]
      val previousEvolution = mock[Evolution[Int, String, String]]
      val nextStage = mock[Stage[String, Long, String]]
      val nextEvolution = mock[Evolution[String, Long, String]]
      inSequence {
        (previousStage.apply _).expects(in).returns(Yield.Some(out, previousStatus, previousEvolution))
        (nextStage.apply _).expects(out).returns(Yield.None(nextStatus, nextEvolution))
      }
      inside(Stage.AndThen(previousStage, nextStage)(in)) { case Yield.None(status, evolution) =>
        status shouldBe previousStatus ++ nextStatus
        val updatedPreviousStage = mock[Stage[Int, String, String]]
        val updatedNextStage = mock[Stage[String, Long, Nothing]]
        inSequence {
          armEvolution(nextEvolution, status, updatedNextStage)
          armEvolution(previousEvolution, status, updatedPreviousStage)
        }
        status(evolution) shouldBe Stage.AndThen(updatedPreviousStage, updatedNextStage)
      }
    }

  it should "call only the first stage and return the correct Yield for None ~> any Yield" in
    forAll { (previousStatus: Status[String], in: Int) =>
      val previousStage = mock[Stage[Int, String, String]]
      val previousEvolution = mock[Evolution[Int, String, String]]
      val nextStage = mock[Stage[String, Long, String]]
      (previousStage.apply _).expects(in).returns(Yield.None(previousStatus, previousEvolution))
      inside(Stage.AndThen(previousStage, nextStage)(in)) { case Yield.None(`previousStatus`, evolution) =>
        val evolvedPreviousStage = mock[Stage[Int, String, String]]
        armEvolution(previousEvolution, previousStatus, evolvedPreviousStage)
        previousStatus(evolution) shouldBe Stage.AndThen(evolvedPreviousStage, nextStage)
      }
    }

  private def armEvolution[I, O, E](
      evolution: Evolution[I, O, E],
      status: Status[E],
      stage: Stage[I, O, E]): (Evolution[I, O, E], Stage[I, O, E]) = {
    status match {
      case Status.Success => (evolution.onSuccess _).expects().returns(stage)
      case Status.Complete => (evolution.onComplete _).expects().returns(stage)
      case Status.Error(_, _) => (evolution.onError _).expects().returns(stage)
    }
    (evolution, stage)
  }

  it should "call the next stage's dispose and then the previous stage's dispose" in {
    val previousStage = mock[Stage[Int, String, String]]
    val nextStage = mock[Stage[String, Long, String]]
    inSequence {
      (nextStage.dispose _).expects()
      (previousStage.dispose _).expects()
    }
    Stage.AndThen(previousStage, nextStage).dispose()
  }

  it should "call the previous stage's dispose even if the next stage's dispose throws" in {
    val previousStage = mock[Stage[Int, String, String]]
    val nextStage = mock[Stage[String, Long, String]]
    val nextException = new Exception("next's stage dispose failed")
    inSequence {
      (nextStage.dispose _).expects().throws(nextException)
      (previousStage.dispose _).expects()
    }
    the[Throwable] thrownBy Stage.AndThen(previousStage, nextStage).dispose() should be theSameInstanceAs nextException
  }

  it should "suppress the previous stage's dispose exception if the next stage's dispose throws" in {
    val previousStage = mock[Stage[Int, String, String]]
    val nextStage = mock[Stage[String, Long, String]]
    val nextException = new Exception("next's stage dispose failed")
    val previousException = new Exception("previous's stage dispose failed")
    inSequence {
      (nextStage.dispose _).expects().throws(nextException)
      (previousStage.dispose _).expects().throws(previousException)
    }
    val exception = the[Throwable] thrownBy Stage.AndThen(previousStage, nextStage).dispose()
    exception should be theSameInstanceAs nextException
    exception.getSuppressed should contain(previousException)
  }
}
