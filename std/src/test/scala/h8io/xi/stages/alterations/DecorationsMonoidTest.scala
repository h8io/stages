package h8io.xi.stages.alterations

import cats.kernel.laws.discipline.MonoidTests
import cats.{Eq, Monoid}
import h8io.xi.stages.{Alteration, AlterationCompose, AlterationOps, Decoration, Stage}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

import scala.annotation.tailrec

class DecorationsMonoidTest extends AnyFunSuite with FunSuiteDiscipline with Checkers {
  private implicit def decorationMonoid[I, O, E]: Monoid[Decoration[I, O, E]] =
    new Monoid[Decoration[I, O, E]] {
      def empty: Decoration[I, O, E] = Identity[I, O, E]
      def combine(x: Decoration[I, O, E], y: Decoration[I, O, E]): Decoration[I, O, E] = x ~> y
    }

  private case class GeneratedAlteration[-IS <: Stage.Any, +OS <: Stage.Any](id: Long) extends Alteration[IS, OS] {
    override def apply(stage: IS): OS = throw new AbstractMethodError
  }

  private implicit def genDecoration[I, O, E]: Arbitrary[Decoration[I, O, E]] =
    Arbitrary(Gen.long map GeneratedAlteration[Stage[I, O, E], Stage[I, O, E]])

  def signature(alteration: Alteration[?, ?]): List[Long] = {
    @tailrec def loop(todo: List[Alteration[?, ?]], acc: List[Long]): List[Long] =
      todo match {
        case Nil => acc
        case Identity :: rest => loop(rest, acc)
        case GeneratedAlteration(id) :: rest => loop(rest, id :: acc)
        case AlterationCompose(previous, next) :: rest => loop(next :: previous :: rest, acc)
        case other => fail(s"Unexpected decoration class: ${other.getClass}, value: $other")
      }
    loop(alteration :: Nil, Nil)
  }

  private implicit def decorationEq[I, O, E]: Eq[Decoration[I, O, E]] = (x, y) => signature(x) == signature(y)

  checkAll("Decoration", MonoidTests[Decoration[Any, Nothing, Nothing]].monoid)
}
