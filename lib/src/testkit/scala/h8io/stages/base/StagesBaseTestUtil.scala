package h8io.stages.base

import h8io.stages.{Evolution, Stage}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

/** Test utilities for specs that verify `h8io.stages.Evolution` wrapping behaviour.
  *
  * Mix this trait into a ScalaTest suite to gain access to `testWrappedEvolution`, which sets up ScalaMock
  * expectations on an inner evolution and asserts that the outer (wrapped) evolution delegates to it correctly.
  *
  * {{{
  * class MyOperatorSpec extends AnyFunSuite with StagesBaseTestUtil {
  *   test("Lift evolution wraps each branch") {
  *     val innerEvolution = mock[Evolution[Int, String, Nothing]]
  *     val wrappedEvolution: Evolution[Int, Option[String], Nothing] = ...
  *     testWrappedEvolution(wrappedEvolution, innerEvolution, Lift(_))
  *   }
  * }
  * }}}
  */
trait StagesBaseTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  /** Asserts that `wrappedEvolution` delegates every branch to `evolution` via the same `alteration` function.
    *
    * For each of the three branches (`onSuccess`, `onComplete`, `onError`), this method:
    *   1. Creates a mock `h8io.stages.Stage` and registers an expectation that the corresponding method on
    *      `evolution` will be called once and return it.
    *   1. Calls the matching method on `wrappedEvolution`.
    *   1. Asserts that the result equals `alteration(mockStage)`.
    *
    * Use this overload when all three branches apply the same alteration.
    *
    * @param wrappedEvolution
    *   the evolution under test (wraps `evolution`)
    * @param evolution
    *   the inner mock evolution
    * @param alteration
    *   the transformation expected to be applied to each branch's stage
    * @tparam II
    *   input type of the inner stages
    * @tparam IO
    *   output type of the inner stages
    * @tparam IE
    *   error type of the inner stages
    * @tparam OI
    *   input type of the wrapped stages
    * @tparam OO
    *   output type of the wrapped stages
    * @tparam OE
    *   error type of the wrapped stages
    */
  def testWrappedEvolution[II, IO, IE, OI, OO, OE](
      wrappedEvolution: Evolution[OI, OO, OE],
      evolution: Evolution[II, IO, IE],
      alteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]]): Unit =
    testWrappedEvolution(wrappedEvolution, evolution, alteration, alteration, alteration)

  /** Asserts that `wrappedEvolution` delegates each branch to `evolution` via the corresponding alteration function.
    *
    * Compared to the single-alteration overload, this variant lets each branch (`onSuccess`, `onComplete`, `onError`)
    * use a different alteration — useful when a wrapped evolution applies different logic per branch.
    *
    * @param wrappedEvolution
    *   the evolution under test
    * @param evolution
    *   the inner mock evolution
    * @param onSuccessAlteration
    *   expected transformation for the `onSuccess` branch
    * @param onCompleteAlteration
    *   expected transformation for the `onComplete` branch
    * @param onErrorAlteration
    *   expected transformation for the `onError` branch
    * @tparam II
    *   input type of the inner stages
    * @tparam IO
    *   output type of the inner stages
    * @tparam IE
    *   error type of the inner stages
    * @tparam OI
    *   input type of the wrapped stages
    * @tparam OO
    *   output type of the wrapped stages
    * @tparam OE
    *   error type of the wrapped stages
    */
  def testWrappedEvolution[II, IO, IE, OI, OO, OE](
      wrappedEvolution: Evolution[OI, OO, OE],
      evolution: Evolution[II, IO, IE],
      onSuccessAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]],
      onCompleteAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]],
      onErrorAlteration: Alteration[Stage[II, IO, IE], Stage[OI, OO, OE]]): Unit = {
    val onSuccessStage = mock[Stage[II, IO, IE]]("onSuccess stage")
    (evolution.onSuccess _).expects().returns(onSuccessStage)
    wrappedEvolution.onSuccess() shouldBe onSuccessAlteration(onSuccessStage)

    val onCompleteStage = mock[Stage[II, IO, IE]]("onComplete stage")
    (evolution.onComplete _).expects().returns(onCompleteStage)
    wrappedEvolution.onComplete() shouldBe onCompleteAlteration(onCompleteStage)

    val onErrorStage = mock[Stage[II, IO, IE]]("onError stage")
    (evolution.onError _).expects().returns(onErrorStage)
    wrappedEvolution.onError() shouldBe onErrorAlteration(onErrorStage)
  }
}
