package h8io.stages

import h8io.stages.Stage.AndThen
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.sql.Timestamp
import java.time.*

class StageTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "~>" should "produce a Stage.AndThen with a stage argument" in {
    val upstream = mock[Stage[String, Long, Nothing]]
    val downstream = mock[Stage[Long, Timestamp, String]]
    upstream ~> downstream shouldBe Stage.AndThen(upstream, downstream)
  }

  "<~" should "produce a Stage.AndThen" in {
    val upstream = mock[Stage[Instant, Int, String]]
    val downstream = mock[Stage[Int, String, Nothing]]
    downstream <~ upstream shouldBe Stage.AndThen(upstream, downstream)
  }

  "AndThen" should "call stages sequentially and return the correct Yield for Some ~> Some" in
    forAll {
      (upstreamStatus: Status[String], downstreamStatus: Status[String], in: Int, upstreamOut: String,
          downstreamOut: Long) =>
        val upstreamStage = mock[Stage[Int, String, String]]
        val upstreamEvolution = mock[Evolution[Int, String, String]]
        val downstreamStage = mock[Stage[String, Long, String]]
        val downstreamEvolution = mock[Evolution[String, Long, String]]
        inSequence {
          (upstreamStage.apply _).expects(in).returns(Yield.Some(upstreamOut, upstreamStatus, upstreamEvolution))
          (downstreamStage.apply _).expects(upstreamOut).returns(
            Yield.Some(downstreamOut, downstreamStatus, downstreamEvolution))
        }
        inside(Stage.AndThen(upstreamStage, downstreamStage)(in)) {
          case Yield.Some(`downstreamOut`, status, evolution) =>
            status shouldBe upstreamStatus.combine(downstreamStatus)
            val updatedUpstreamStage = mock[Stage[Int, String, String]]
            val updatedDownstreamStage = mock[Stage[String, Long, Nothing]]
            inSequence {
              evolutionMock(downstreamEvolution, status, updatedDownstreamStage)
              evolutionMock(upstreamEvolution, status, updatedUpstreamStage)
            }
            evolution.evolve(status) shouldBe Stage.AndThen(updatedUpstreamStage, updatedDownstreamStage)
        }
    }

  it should "call stages sequentially and return the correct Yield for Some ~> None" in
    forAll { (upstreamStatus: Status[String], downstreamStatus: Status[String], in: Int, out: String) =>
      val upstreamStage = mock[Stage[Int, String, String]]
      val upstreamEvolution = mock[Evolution[Int, String, String]]
      val downstreamStage = mock[Stage[String, Long, String]]
      val downstreamEvolution = mock[Evolution[String, Long, String]]
      inSequence {
        (upstreamStage.apply _).expects(in).returns(Yield.Some(out, upstreamStatus, upstreamEvolution))
        (downstreamStage.apply _).expects(out).returns(Yield.None(downstreamStatus, downstreamEvolution))
      }
      inside(Stage.AndThen(upstreamStage, downstreamStage)(in)) { case Yield.None(status, evolution) =>
        status shouldBe upstreamStatus.combine(downstreamStatus)
        val updatedUpstreamStage = mock[Stage[Int, String, String]]
        val updatedDownstreamStage = mock[Stage[String, Long, Nothing]]
        inSequence {
          evolutionMock(downstreamEvolution, status, updatedDownstreamStage)
          evolutionMock(upstreamEvolution, status, updatedUpstreamStage)
        }
        evolution.evolve(status) shouldBe Stage.AndThen(updatedUpstreamStage, updatedDownstreamStage)
      }
    }

  it should "call only the first stage and return the correct Yield for None ~> any Yield" in
    forAll { (upstreamStatus: Status[String], in: Int) =>
      val upstreamStage = mock[Stage[Int, String, String]]
      val upstreamEvolution = mock[Evolution[Int, String, String]]
      val downstreamStage = mock[Stage[String, Long, String]]
      val downstreamEvolution = mock[Evolution[String, Long, String]]
      inSequence {
        (upstreamStage.apply _).expects(in).returns(Yield.None(upstreamStatus, upstreamEvolution))
        (downstreamStage.skip _).expects().returns(downstreamEvolution)
      }
      inside(Stage.AndThen(upstreamStage, downstreamStage)(in)) { case Yield.None(`upstreamStatus`, evolution) =>
        val evolvedDownstreamStage = mock[Stage[String, Long, String]]
        evolutionMock(downstreamEvolution, upstreamStatus, evolvedDownstreamStage)
        val evolvedUpstreamStage = mock[Stage[Int, String, String]]
        evolutionMock(upstreamEvolution, upstreamStatus, evolvedUpstreamStage)
        evolution.evolve(upstreamStatus) shouldBe evolvedUpstreamStage ~> evolvedDownstreamStage
      }
    }

  it should "sequentially call skip methods for both stages" in {
    val upstreamStage = mock[Stage[Int, String, String]]
    val upstreamEvolution = mock[Evolution[Int, String, String]]
    val downstreamStage = mock[Stage[String, Long, String]]
    val downstreamEvolution = mock[Evolution[String, Long, String]]
    inSequence {
      (upstreamStage.skip _).expects().returns(upstreamEvolution)
      (downstreamStage.skip _).expects().returns(downstreamEvolution)
    }
    testEvolutionComposition(
      AndThen(upstreamStage, downstreamStage).skip(),
      upstreamEvolution,
      downstreamEvolution,
      AndThen[Int, String, Long, String])
  }
}
