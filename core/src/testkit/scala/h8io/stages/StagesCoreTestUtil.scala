package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import org.scalatest.matchers.should.Matchers

trait StagesCoreTestUtil extends MockFactory with Matchers {
  self: TestSuite =>

  def evolutionMock[I, O, E](evolution: Evolution[I, O, E], status: Status[E], stage: Stage[I, O, E]): Unit =
    status match {
      case Status.Success => (evolution.onSuccess _).expects().returns(stage)
      case Status.Complete => (evolution.onComplete _).expects().returns(stage)
      case _: Status.Error[E] => (evolution.onError _).expects().returns(stage)
    }
}
