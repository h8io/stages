package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

/** Test utilities for specs that exercise the core `stages` abstractions.
  *
  * Mix this trait into a ScalaTest suite (alongside `MockFactory` and `Matchers`, which are already extended here) to
  * get access to helpers that set up mock expectations on [[Evolution]] instances.
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
    *   - [[Status.Success]] → `evolution.onSuccess()`
    *   - [[Status.Complete]] → `evolution.onComplete()`
    *   - [[Status.Error]] → `evolution.onError()`
    *
    * @param evolution
    *   the mock [[Evolution]] on which the expectation is registered
    * @param status
    *   the [[Status]] that determines which branch to expect
    * @param stage
    *   the [[Stage]] that the expected branch should return
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
}
