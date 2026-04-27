package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

/** Test utilities for specs that exercise the core `stages` abstractions.
  *
  * Mix this trait into a ScalaTest suite (alongside `MockFactory` and `Matchers`, which are already extended here) to
  * get access to helpers that set up mock expectations on `Evolution` instances.
  *
  * {{{
  * class MySpec extends AnyFunSuite with StagesCoreTestUtil {
  *   test("evolution selects stage by status") {
  *     val evo   = mock[Evolution[String, Int, String]]
  *     val stage = mock[Stage[String, Int, String]]
  *     evolutionMock(evo, Status.Success, stage)
  *     // ... invoke code under test that calls evo(Status.Success) ...
  *   }
  * }
  * }}}
  */
trait StagesCoreTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  /** Sets up a ScalaMock expectation on `evolution` so that `apply(status)` is expected to be called exactly once and
    * will return `stage`.
    *
    * @param evolution
    *   the mock `Evolution` on which the expectation is registered
    * @param status
    *   the `Status` that the expectation matches
    * @param stage
    *   the `Stage` that the expected call should return
    * @tparam I
    *   the stage input type
    * @tparam O
    *   the stage output type
    * @tparam E
    *   the error type
    */
  def evolutionMock[I, O, E](evolution: Evolution[I, O, E], status: Status[E], stage: Stage[I, O, E]): Unit =
    (evolution.apply(_: Status[E])).expects(status).returns(stage)

  /** Asserts that `evolution` correctly composes `leftEvolution` and `rightEvolution` for all status values.
    *
    * Tests `apply(Status.Success)` and `apply(Status.complete)`, verifying that each result equals
    * `compose(leftStage, rightStage)` where each sub-evolution is called with the same status.
    *
    * All calls are ordered with `inSequence` to ensure deterministic expectation matching.
    *
    * @param composition
    *   the composed evolution under test
    * @param leftEvolution
    *   the mock left evolution whose continuations supply the left continuation stages
    * @param rightEvolution
    *   the mock right evolution whose continuations supply the right continuation stages
    * @param compose
    *   the function that reconstructs the expected composed stage from its two halves
    * @tparam LI
    *   the left evolution input type
    * @tparam LO
    *   the left evolution output type
    * @tparam RI
    *   the right evolution input type
    * @tparam RO
    *   the right evolution output type
    * @tparam I
    *   the composed evolution input type
    * @tparam O
    *   the composed evolution output type
    * @tparam E
    *   the error type
    */
  def testEvolutionComposition[LI, LO, RI, RO, I, O, E](
      composition: Evolution[I, O, E],
      leftEvolution: Evolution[LI, LO, E],
      rightEvolution: Evolution[RI, RO, E],
      compose: (Stage[LI, LO, E], Stage[RI, RO, E]) => Stage[I, O, E]): Unit =
    inSequence {
      val successLeftStage = mock[Stage[LI, LO, E]]
      val successRightStage = mock[Stage[RI, RO, E]]
      (rightEvolution.apply _).expects(Status.Success).returns(successRightStage)
      (leftEvolution.apply _).expects(Status.Success).returns(successLeftStage)
      composition(Status.Success) shouldBe compose(successLeftStage, successRightStage)

      val completeLeftStage = mock[Stage[LI, LO, E]]
      val completeRightStage = mock[Stage[RI, RO, E]]
      val error = mockComplete()
      (rightEvolution.apply _).expects(error).returns(completeRightStage)
      (leftEvolution.apply _).expects(error).returns(completeLeftStage)
      composition(error) shouldBe compose(completeLeftStage, completeRightStage)

      (rightEvolution.dispose _).expects()
      (leftEvolution.dispose _).expects()
      noException should be thrownBy composition.dispose()
    }

  /** Asserts that `altered` correctly maps every continuation of `evolution` through `f`, and that disposal delegates.
    *
    * Tests `apply(Status.Success)` and `apply(Status.complete)`, verifying that each result equals `f(mockStage)` where
    * `mockStage` is the stage returned by the inner `evolution`.
    *
    * Also verifies that `altered.dispose()` delegates to `evolution.dispose()`.
    *
    * @param altered
    *   the evolution under test (wraps `evolution` via `f`)
    * @param evolution
    *   the mock inner evolution
    * @param f
    *   the transformation expected to be applied to each continuation's stage
    * @tparam AI
    *   input type of the inner stages
    * @tparam AO
    *   output type of the inner stages
    * @tparam AE
    *   error type of the inner stages
    * @tparam I
    *   input type of the altered stages
    * @tparam O
    *   output type of the altered stages
    * @tparam E
    *   error type of the altered stages
    */
  def testAlteredEvolution[AI, AO, AE, I, O, E](
      altered: Evolution[I, O, E],
      evolution: Evolution[AI, AO, AE],
      f: Stage[AI, AO, AE] => Stage[I, O, E]): Unit =
    inSequence {
      val successStage = mock[Stage[AI, AO, AE]]
      (evolution.apply(_: Status[AE])).expects(Status.Success).returns(successStage)
      altered(Status.Success) shouldBe f(successStage)

      val completeStage = mock[Stage[AI, AO, AE]]
      val complete = mockComplete()
      (evolution.apply _).expects(complete).returns(completeStage)
      altered(complete) shouldBe f(completeStage)

      (evolution.dispose _).expects()
      noException should be thrownBy altered.dispose()
    }

  // Intentionally mocked Seq: the test must fail if any Seq method is touched.
  // ScalaMock emits a Scala 2.13 deprecation warning for Iterable.stringPrefix.
  def mockComplete(): Status.Complete[?] =
    Status.Complete(mock[Seq[AnyRef]]: @nowarn("cat=deprecation&msg=.*stringPrefix.*"))
}
