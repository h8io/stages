package h8io.stages

import cats.implicits.catsSyntaxSemigroup
import cats.kernel.laws.discipline.MonoidTests
import cats.{Eq, Monoid, Semigroup}
import h8io.stages.Stage.Endo
import org.scalacheck.{Arbitrary, Prop, Shrink, Test}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

import scala.annotation.tailrec

class EndoStageMonoidTest
    extends AnyFunSuite
    with MockFactory
    with FunSuiteDiscipline
    with Checkers
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  private val parameters = Test.Parameters.default

  private object Identity extends Stage[Any, Any, Nothing] {
    override def apply(in: Any): Yield.Some[Any, Any, Nothing] = Yield.Some(in, Status.Success, evolution)

    override def skip(): Evolution[Any, Any, Nothing] = evolution

    private val evolution = new Evolution[Any, Any, Nothing] {
      override def apply(status: Status[?]): Stage[Any, Any, Nothing] = Identity
      def dispose(): Unit = ()
    }

    def apply[T]: Endo[T, Nothing] = this.asInstanceOf[Stage.Endo[T, Nothing]]
  }

  private implicit def stageMonoid[T, E]: Monoid[Stage.Endo[T, E]] =
    new Monoid[Stage.Endo[T, E]] {
      def empty: Stage.Endo[T, E] = Identity[T]
      def combine(x: Stage.Endo[T, E], y: Stage.Endo[T, E]): Stage.Endo[T, E] = x ~> y
    }

  private implicit def genStage[T: Arbitrary: Semigroup, E: Arbitrary]: Arbitrary[Stage.Endo[T, E]] =
    Arbitrary {
      for {
        prefix <- Arbitrary.arbitrary[T]
        suffix <- Arbitrary.arbitrary[T]
        status <- Arbitrary.arbitrary[Status[E]]
      } yield new Stage.Endo[T, E] {
        self =>
        def apply(in: T): Yield[T, T, E] = Yield.Some(prefix |+| in |+| suffix, status, evolution)

        override def skip(): Evolution[T, T, E] = evolution

        private val evolution = new Evolution[T, T, E] {
          override def apply(status: Status[?]): Stage[T, T, E] = self
          override def dispose(): Unit = {}
        }

        override def toString(): String = s"Stage.Endo: $prefix + _ + $suffix"
      }
    }

  private def toList[E](stage: Stage[?, ?, E]): List[Stage[?, ?, E]] = {
    @tailrec def loop(todo: List[Stage[?, ?, E]], acc: List[Stage[?, ?, E]]): List[Stage[?, ?, E]] =
      todo match {
        case Nil => acc
        case Stage.AndThen(upstream, downstream) :: rest => loop(downstream :: upstream :: rest, acc)
        case Identity :: rest => loop(rest, acc)
        case other :: rest => loop(rest, other :: acc)
      }
    loop(stage :: Nil, Nil)
  }

  private def toTuple[IO, E](evolution: Evolution[IO, IO, E]): (List[Stage.Any], List[Stage.Any]) =
    (toList(evolution(Status.Success)), toList(evolution(mockComplete())))

  private def toTuple[T, E](yld: Yield[T, T, E]): Product =
    yld match {
      case Yield.Some(out, status, evolution) => (out, status, toTuple(evolution))
      case Yield.None(status, evolution) => (status, toTuple(evolution))
    }

  private implicit def stageEq[T: Arbitrary: Shrink, E]: Eq[Stage.Endo[T, E]] =
    (x: Stage.Endo[T, E], y: Stage.Endo[T, E]) =>
      Test.check(parameters, Prop.forAll((in: T) => toTuple(x(in)) == toTuple(y(in)))).passed

  checkAll("Stage[Int, String]", MonoidTests[Stage.Endo[Int, String]].monoid)

  checkAll("Stage[String, Int]", MonoidTests[Stage.Endo[String, Int]].monoid)
}
