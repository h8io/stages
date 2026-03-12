package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

trait StagesCoreTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  def onDoneMock[I, O, E](onDone: OnDone[I, O, E], signal: Signal[E], stage: Stage[I, O, E]): Unit =
    signal match {
      case Signal.Success => (onDone.onSuccess _).expects().returns(stage)
      case Signal.Complete => (onDone.onComplete _).expects().returns(stage)
      case _: Signal.Error[E] => (onDone.onError _).expects().returns(stage)
    }
}
