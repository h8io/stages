package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

/** Test utilities for specs that exercise the core `stages` abstractions.
  *
  * Mix this trait into a ScalaTest suite (alongside `MockFactory` and `Matchers`, which are already extended here) to
  * get access to helpers that set up mock expectations on `Evolution` instances.
  *
  * {{{
  * class MySpec extends AnyFunSuite with StagesCoreTestUtil {
  *   test("evolution selects onSuccess when status is Success") {
  *     val evo   = mock[Evolution[String, Int, String]]
  *     val stage = mock[Stage[String, Int, String]]
  *     evolutionMock(evo, Status.Success, stage)
  *     // ... invoke code under test that calls evo.onSuccess() ...
  *   }
  * }
  * }}}
  */
trait StagesCoreTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  /** Sets up a ScalaMock expectation on `evolution` so that the branch corresponding to `status` is expected to be
    * called exactly once and will return `stage`.
    *
    * The mapping between status and branch is:
    *   - `Status.Success` → `evolution.onSuccess()`
    *   - `Status.Complete` → `evolution.onComplete()`
    *   - `Status.Error` → `evolution.onError()`
    *
    * @param evolution
    *   the mock `Evolution` on which the expectation is registered
    * @param status
    *   the `Status` that determines which branch to expect
    * @param stage
    *   the `Stage` that the expected branch should return
    * @tparam I
    *   the stage input type
    * @tparam O
    *   the stage output type
    * @tparam E
    *   the error type
    */
  def evolutionMock[I, O, E](evolution: Evolution[I, O, E], status: Status[E], stage: Stage[I, O, E]): Unit =
    status match {
      case Status.Success => (evolution.onSuccess _).expects().returns(stage)
      case Status.Complete => (evolution.onComplete _).expects().returns(stage)
      case _: Status.Error[E] => (evolution.onError _).expects().returns(stage)
    }

  /** Asserts that `evolution` correctly composes `leftEvolution` and `rightEvolution` for all three status branches.
    *
    * For each branch (`onSuccess`, `onComplete`, `onError`) the helper:
    *   1. Registers ScalaMock expectations on both `leftEvolution` and `rightEvolution`.
    *   1. Calls the corresponding branch on `evolution`.
    *   1. Asserts the returned stage equals `compose(leftStage, rightStage)`.
    *
    * All six branch calls are ordered with `inSequence` to ensure deterministic expectation matching.
    *
    * @param composition
    *   the composed evolution under test
    * @param leftEvolution
    *   the mock left evolution whose branches supply the left continuation stages
    * @param rightEvolution
    *   the mock right evolution whose branches supply the right continuation stages
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
      val onSuccessLeftStage = mock[Stage[LI, LO, E]]
      val onSuccessRightStage = mock[Stage[RI, RO, E]]
      (rightEvolution.onSuccess _).expects().returns(onSuccessRightStage)
      (leftEvolution.onSuccess _).expects().returns(onSuccessLeftStage)
      composition.onSuccess() shouldBe compose(onSuccessLeftStage, onSuccessRightStage)

      val onCompleteLeftStage = mock[Stage[LI, LO, E]]
      val onCompleteRightStage = mock[Stage[RI, RO, E]]
      (rightEvolution.onComplete _).expects().returns(onCompleteRightStage)
      (leftEvolution.onComplete _).expects().returns(onCompleteLeftStage)
      composition.onComplete() shouldBe compose(onCompleteLeftStage, onCompleteRightStage)

      val onErrorLeftStage = mock[Stage[LI, LO, E]]
      val onErrorRightStage = mock[Stage[RI, RO, E]]
      (rightEvolution.onError _).expects().returns(onErrorRightStage)
      (leftEvolution.onError _).expects().returns(onErrorLeftStage)
      composition.onError() shouldBe compose(onErrorLeftStage, onErrorRightStage)
    }

  def testAlteredEvolution[AI, AO, AE, I, O, E](
      altered: Evolution[I, O, E],
      evolution: Evolution[AI, AO, AE],
      f: Stage[AI, AO, AE] => Stage[I, O, E]): Unit =
    inSequence {
      val onSuccessStage = mock[Stage[AI, AO, AE]]
      (evolution.onSuccess _).expects().returns(onSuccessStage)
      altered.onSuccess() shouldBe f(onSuccessStage)

      val onCompleteStage = mock[Stage[AI, AO, AE]]
      (evolution.onComplete _).expects().returns(onCompleteStage)
      altered.onComplete() shouldBe f(onCompleteStage)

      val onErrorStage = mock[Stage[AI, AO, AE]]
      (evolution.onError _).expects().returns(onErrorStage)
      altered.onError() shouldBe f(onErrorStage)
    }
}
