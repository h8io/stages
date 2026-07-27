package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

/** Assertions for the recurring shapes of [[Evolution]] found in operators, so that a suite only spells out what is
  * specific to the operator under test.
  *
  * Each helper drives the evolution through both status branches — `Status.Success` and a `Complete` — since that
  * choice is exactly what an evolution exists to make. Mix it into a suite alongside its own `MockFactory` usage; it is
  * shared with the other modules through the `core % "test->testkit"` dependency.
  */
trait StagesCoreTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  /** Expects `evolution` to be evolved exactly once, on `status`, and to answer with `stage`. */
  def evolutionMock[I, O, E](evolution: Evolution[I, O, E], status: Status[E], stage: Stage[I, O, E]): Unit =
    (evolution.evolve(_: Status[E])).expects(status).returns(stage)

  /** Asserts that `composition` evolves both sides on the status it is given and recombines their continuations with
    * `compose`, and that disposing it disposes both.
    *
    * The expectations are ordered: `rightEvolution` before `leftEvolution`, the order every composed evolution in the
    * library releases and evolves its halves in.
    */
  def testEvolutionComposition[LI, LO, RI, RO, I, O, E](
      composition: Evolution[I, O, E],
      leftEvolution: Evolution[LI, LO, E],
      rightEvolution: Evolution[RI, RO, E],
      compose: (Stage[LI, LO, E], Stage[RI, RO, E]) => Stage[I, O, E]): Unit =
    inSequence {
      val successLeftStage = mock[Stage[LI, LO, E]]
      val successRightStage = mock[Stage[RI, RO, E]]
      (rightEvolution.evolve _).expects(Status.Success).returns(successRightStage)
      (leftEvolution.evolve _).expects(Status.Success).returns(successLeftStage)
      composition.evolve(Status.Success) shouldBe compose(successLeftStage, successRightStage)

      val completeLeftStage = mock[Stage[LI, LO, E]]
      val completeRightStage = mock[Stage[RI, RO, E]]
      val error = mockComplete()
      (rightEvolution.evolve _).expects(error).returns(completeRightStage)
      (leftEvolution.evolve _).expects(error).returns(completeLeftStage)
      composition.evolve(error) shouldBe compose(completeLeftStage, completeRightStage)

      (rightEvolution.dispose _).expects()
      (leftEvolution.dispose _).expects()
      noException should be thrownBy composition.dispose()
    }

  /** Asserts that `altered` passes every continuation of `evolution` through `f` and delegates disposal to it —
    * the contract of [[Evolution.map]] and of the operators built on it.
    */
  def testMappedEvolution[MI, MO, ME, I, O, E](
      altered: Evolution[I, O, E],
      evolution: Evolution[MI, MO, ME],
      f: Stage[MI, MO, ME] => Stage[I, O, E]): Unit =
    inSequence {
      val successStage = mock[Stage[MI, MO, ME]]
      (evolution.evolve(_: Status[ME])).expects(Status.Success).returns(successStage)
      altered.evolve(Status.Success) shouldBe f(successStage)

      val completeStage = mock[Stage[MI, MO, ME]]
      val complete = mockComplete()
      (evolution.evolve _).expects(complete).returns(completeStage)
      altered.evolve(complete) shouldBe f(completeStage)

      (evolution.dispose _).expects()
      noException should be thrownBy altered.dispose()
    }

  /** Asserts that `evolution` answers `stage` whatever the status — the barrier the `cycles` operators put between
    * the enclosing pipeline's status and their inner stage.
    */
  def testConstEvolution[I, O, E](evolution: Evolution[I, O, E], stage: Stage[I, O, E]): Unit = {
    evolution.evolve(Status.Success) shouldBe stage
    evolution.evolve(mockComplete()) shouldBe stage
  }

  /** A `Complete` whose errors are a mock: it matches by reference in expectations, so a test can tell one `Complete`
    * from another without inventing error values.
    */
  def mockComplete(): Status.Complete[?] =
    Status.Complete(mock[Seq[Any]]: @nowarn("cat=deprecation&msg=.*stringPrefix.*"))
}
