package h8io.xi.stages

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

  "outcome" should "run onDone and return Outcome.Some" in
    forAll { (in: Long, yieldSupplier: OnDoneToYieldSome[Long, String, UUID]) =>
      val stage = mock[Stage[Long, String, UUID]]
      val onDone = mock[OnDone[Long, String, UUID]]
      val yld = yieldSupplier(onDone)
      val evolved = mock[Stage[Long, String, UUID]]
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        onDoneMock(onDone, yld.signal, evolved)
      }
      inside(stage.outcome(in)) { case Outcome.Some(yld.out, yld.signal, dispose) =>
        (evolved.dispose _).expects()
        dispose()
      }
    }

  it should "run onDone and return Outcome.None" in
    forAll { (in: Instant, yieldSupplier: OnDoneToYieldNone[Instant, Boolean, Long]) =>
      val stage = mock[Stage[Instant, Boolean, Long]]
      val onDone = mock[OnDone[Instant, Boolean, Long]]
      val yld = yieldSupplier(onDone)
      val evolved = mock[Stage[Instant, Boolean, Long]]
      inSequence {
        (stage.apply _).expects(in).returns(yld)
        onDoneMock(onDone, yld.signal, evolved)
      }
      inside(stage.outcome(in)) { case Outcome.None(yld.signal, dispose) =>
        (evolved.dispose _).expects()
        dispose()
      }
    }

  "~>" should "produce Stage.AndThen object with a stage argument" in {
    val previous = mock[Stage[String, Long, Nothing]]
    val next = mock[Stage[Long, Timestamp, String]]
    previous ~> next shouldBe Stage.AndThen(previous, next)
  }

  it should "produce a alteration with a alteration argument" in {
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

  "<~" should "produce Stage.AndThen object" in {
    val previous = mock[Stage[Instant, Int, String]]
    val next = mock[Stage[Int, String, Nothing]]
    next <~ previous shouldBe Stage.AndThen(previous, next)
  }

  "|>" should "apply alteration to stage" in {
    val stage = mock[Stage[ZoneOffset, OffsetDateTime, Exception]]
    val alteration = mock[Alteration[Stage[ZoneOffset, OffsetDateTime, Exception], Stage[UUID, Instant, Long]]]
    val out = mock[Stage[UUID, Instant, Long]]
    (alteration.apply _).expects(stage).returns(out)
    stage |> alteration shouldBe out
  }

  "alteration" should "be a leftAlteration" in {
    val left = mock[Stage[Int, Long, Nothing]]
    val right = mock[Stage[Long, Duration, Exception]]
    left.alteration(right) shouldBe left ~> right
  }

  "leftAlteration" should "produce a composition with predefined left operand as an be alteration" in {
    val left = mock[Stage[Int, Long, Nothing]]
    val right = mock[Stage[Long, Duration, Exception]]
    val alteration: Alteration[Stage[Long, Duration, Exception], Stage[Int, Duration, Exception]] = left.leftAlteration
    alteration(right) shouldBe left ~> right
  }

  "rightAlteration" should "produce a composition with predefined right operand as an be alteration" in {
    val left = mock[Stage[Int, Long, Nothing]]
    val right = mock[Stage[Long, Duration, Exception]]
    val alteration: Alteration[Stage[Int, Long, Nothing], Stage[Int, Duration, Exception]] = right.rightAlteration
    alteration(left) shouldBe left ~> right
  }

  "Default skip method" should "return an idempotent OnDone object" in {
    val stage: Stage[Any, Nothing, Nothing] = new Stage[Any, Nothing, Nothing] {
      def apply(in: Any): Yield[Any, Nothing, Nothing] = fail("apply should not be called")

      override def onSuccess(): Stage[Any, Nothing, Nothing] = mock[Stage[Any, Nothing, Nothing]]
      override def onComplete(): Stage[Any, Nothing, Nothing] = mock[Stage[Any, Nothing, Nothing]]
      override def onError(): Stage[Any, Nothing, Nothing] = mock[Stage[Any, Nothing, Nothing]]
    }
    val onDone = stage.skip
    onDone.onSuccess() should be theSameInstanceAs stage
    onDone.onComplete() should be theSameInstanceAs stage
    onDone.onError() should be theSameInstanceAs stage
  }

  "Default OnDone methods" should "return self" in {
    val stage: Stage[Any, Nothing, Nothing] = _ => fail("apply should not be called")
    stage.onSuccess() should be theSameInstanceAs stage
    stage.onComplete() should be theSameInstanceAs stage
    stage.onError() should be theSameInstanceAs stage
  }

  "AndThen" should "call sequentially stages and return the correct Yield for Some ~> Some" in
    forAll {
      (previousSignal: Signal[String], nextSignal: Signal[String], in: Int, previousOut: String, nextOut: Long) =>
        val previousStage = mock[Stage[Int, String, String]]
        val previousOnDone = mock[OnDone[Int, String, String]]
        val nextStage = mock[Stage[String, Long, String]]
        val nextOnDone = mock[OnDone[String, Long, String]]
        inSequence {
          (previousStage.apply _).expects(in).returns(Yield.Some(previousOut, previousSignal, previousOnDone))
          (nextStage.apply _).expects(previousOut).returns(Yield.Some(nextOut, nextSignal, nextOnDone))
        }
        inside(Stage.AndThen(previousStage, nextStage)(in)) { case Yield.Some(`nextOut`, signal, onDone) =>
          signal shouldBe previousSignal ++ nextSignal
          val updatedPreviousStage = mock[Stage[Int, String, String]]
          val updatedNextStage = mock[Stage[String, Long, Nothing]]
          inSequence {
            armOnDone(nextOnDone, signal, updatedNextStage)
            armOnDone(previousOnDone, signal, updatedPreviousStage)
          }
          signal(onDone) shouldBe Stage.AndThen(updatedPreviousStage, updatedNextStage)
        }
    }

  it should "call sequentially stages and return the correct Yield for Some ~> None" in
    forAll { (previousSignal: Signal[String], nextSignal: Signal[String], in: Int, out: String) =>
      val previousStage = mock[Stage[Int, String, String]]
      val previousOnDone = mock[OnDone[Int, String, String]]
      val nextStage = mock[Stage[String, Long, String]]
      val nextOnDone = mock[OnDone[String, Long, String]]
      inSequence {
        (previousStage.apply _).expects(in).returns(Yield.Some(out, previousSignal, previousOnDone))
        (nextStage.apply _).expects(out).returns(Yield.None(nextSignal, nextOnDone))
      }
      inside(Stage.AndThen(previousStage, nextStage)(in)) { case Yield.None(signal, onDone) =>
        signal shouldBe previousSignal ++ nextSignal
        val updatedPreviousStage = mock[Stage[Int, String, String]]
        val updatedNextStage = mock[Stage[String, Long, Nothing]]
        inSequence {
          armOnDone(nextOnDone, signal, updatedNextStage)
          armOnDone(previousOnDone, signal, updatedPreviousStage)
        }
        signal(onDone) shouldBe Stage.AndThen(updatedPreviousStage, updatedNextStage)
      }
    }

  it should "call the first stage only and return the correct Yield for None ~> any Yield" in
    forAll { (previousSignal: Signal[String], in: Int) =>
      val previousStage = mock[Stage[Int, String, String]]
      val previousOnDone = mock[OnDone[Int, String, String]]
      val nextStage = mock[Stage[String, Long, String]]
      inSequence {
        (previousStage.apply _).expects(in).returns(Yield.None(previousSignal, previousOnDone))
        (() => nextStage.skip).expects().returns(nextStage)
      }
      inside(Stage.AndThen(previousStage, nextStage)(in)) { case Yield.None(`previousSignal`, onDone) =>
        val evolvedPreviousStage = mock[Stage[Int, String, String]]
        val evolvedNextStage = mock[Stage[String, Long, String]]
        inSequence {
          armOnDone(nextStage, previousSignal, evolvedNextStage)
          armOnDone(previousOnDone, previousSignal, evolvedPreviousStage)
        }
        previousSignal(onDone) shouldBe Stage.AndThen(evolvedPreviousStage, evolvedNextStage)
      }
    }

  private def armOnDone[I, O, E](
      onDone: OnDone[I, O, E],
      signal: Signal[E],
      stage: Stage[I, O, E]): (OnDone[I, O, E], Stage[I, O, E]) = {
    signal match {
      case Signal.Success => (onDone.onSuccess _).expects().returns(stage)
      case Signal.Complete => (onDone.onComplete _).expects().returns(stage)
      case Signal.Error(_, _) => (onDone.onError _).expects().returns(stage)
    }
    (onDone, stage)
  }

  it should "call the method dispose in the reversed order" in {
    val previousStage = mock[Stage[Int, String, String]]
    val nextStage = mock[Stage[String, Long, String]]
    inSequence {
      (nextStage.dispose _).expects()
      (previousStage.dispose _).expects()
    }
    Stage.AndThen(previousStage, nextStage).dispose()
  }
}
