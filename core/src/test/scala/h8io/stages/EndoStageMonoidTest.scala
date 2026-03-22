package h8io.stages

import cats.implicits.catsSyntaxSemigroup
import cats.kernel.laws.discipline.MonoidTests
import cats.{Eq, Monoid, Semigroup}
import h8io.stages.Stage.Endo
import org.scalacheck.{Arbitrary, Prop, Shrink, Test}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

import scala.annotation.tailrec

class EndoStageMonoidTest extends AnyFunSuite with FunSuiteDiscipline with Checkers with StagesCoreArbitraries {
  private val parameters = Test.Parameters.default

  private object Identity extends Stage[Any, Any, Nothing] with Evolution[Any, Any, Nothing] {
    override def apply(in: Any): Yield.Some[Any, Any, Nothing] = Yield.Some(in, Status.Success, this)

    override def onSuccess(): Stage[Any, Any, Nothing] = this
    override def onComplete(): Stage[Any, Any, Nothing] = this
    override def onError(): Stage[Any, Any, Nothing] = this

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
      } yield new Stage.Endo[T, E] with Evolution[T, T, E] {
        def apply(in: T): Yield[T, T, E] = Yield.Some(prefix |+| in |+| suffix, status, this)

        override def toString(): String = s"Stage.Endo: $prefix + _ + $suffix"

        override def onSuccess(): Stage[T, T, E] = this

        override def onComplete(): Stage[T, T, E] = this

        override def onError(): Stage[T, T, E] = this
      }
    }

  private def toList[E](stage: Stage[?, ?, E]): List[Stage[?, ?, E]] = {
    @tailrec def loop(todo: List[Stage[?, ?, E]], acc: List[Stage[?, ?, E]]): List[Stage[?, ?, E]] =
      todo match {
        case Nil => acc
        case Stage.AndThen(previous, next) :: rest => loop(next :: previous :: rest, acc)
        case Identity :: rest => loop(rest, acc)
        case other :: rest => loop(rest, other :: acc)
      }
    loop(stage :: Nil, Nil)
  }

  private def toTuple[T, E](evolution: Evolution[T, T, E]): (List[Stage.Any], List[Stage.Any], List[Stage.Any]) =
    (toList(evolution.onSuccess()), toList(evolution.onError()), toList(evolution.onComplete()))

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
