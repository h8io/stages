package h8io.xi.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlteratorTest extends AnyFlatSpec with Matchers with MockFactory {
  "Alterator's dispose" should "call underlying stage's dispose method" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    (stage.dispose _).expects()
    noException should be thrownBy new Decorator[Any, Nothing, Nothing] {
      val alterand: Stage[Any, Nothing, Nothing] = stage
      def apply(in: Any): Yield[Any, Nothing, Nothing] = throw new NoSuchMethodError
    }.dispose()
  }
}
