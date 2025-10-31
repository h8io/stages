package h8io.xi.stages.std

import cats.implicits.catsSyntaxSemigroup
import cats.kernel.laws.discipline.MonoidTests
import cats.{Eq, Monoid, Semigroup}
import h8io.xi.stages.*
import org.scalacheck.{Arbitrary, Prop, Shrink, Test}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

import scala.annotation.tailrec

class EndoStagesMonoidTest extends AnyFunSuite with FunSuiteDiscipline with Checkers with StagesCoreArbitraries {
  private val parameters = Test.Parameters.default

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
        signal <- Arbitrary.arbitrary[Signal[E]]
      } yield new Stage.Endo[T, E] {
        def apply(in: T): Yield[T, T, E] = Yield.Some(prefix |+| in |+| suffix, signal, this)

        override def toString(): String = s"Stage.Endo: $prefix + _ + $suffix"
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

  private def toTuple[T, E](onDone: OnDone[T, T, E]): (List[Stage.Any], List[Stage.Any], List[Stage.Any]) =
    (toList(onDone.onSuccess()), toList(onDone.onError()), toList(onDone.onComplete()))

  private def toTuple[T, E](yld: Yield[T, T, E]): Product =
    yld match {
      case Yield.Some(out, signal, onDone) => (out, signal, toTuple(onDone))
      case Yield.None(signal, onDone) => (signal, toTuple(onDone))
    }

  private implicit def stageEq[T: Arbitrary: Shrink, E]: Eq[Stage.Endo[T, E]] =
    (x: Stage.Endo[T, E], y: Stage.Endo[T, E]) =>
      Test.check(parameters, Prop.forAll((in: T) => toTuple(x(in)) == toTuple(y(in)))).passed

  checkAll("Stage[Int, String]", MonoidTests[Stage.Endo[Int, String]].monoid)

  checkAll("Stage[String, Int]", MonoidTests[Stage.Endo[String, Int]].monoid)
}
