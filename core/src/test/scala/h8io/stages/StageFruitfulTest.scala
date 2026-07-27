package h8io.stages

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageFruitfulTest extends AnyFlatSpec with Matchers {

  /** Passes its input through unchanged, forever. */
  private final class Echo[T] extends Stage.Fruitful[T, T, Nothing] with Evolution.Fruitful[T, T, Nothing] {
    override def apply(in: T): Yield.Some.Fruitful[T, T, Nothing] = Yield.Some.Fruitful(in, Status.Success, this)
    override def skip(): Evolution.Fruitful[T, T, Nothing] = this
    override def evolve(status: Status[?]): Stage.Fruitful[T, T, Nothing] = this
    override def dispose(): Unit = ()
  }

  private val plain: Stage[Int, Int, Nothing] = new Echo[Int] ~> (new Echo[Int]: Stage[Int, Int, Nothing])

  "~>" should "compose fruitful with fruitful into a fruitful stage" in {
    val composed: Stage.Fruitful[Int, Int, Nothing] = new Echo[Int] ~> new Echo[Int]
    composed shouldBe a[Stage.AndThen.Fruitful[?, ?, ?, ?]]
  }

  it should "fall back to a plain composition when either operand is not fruitful" in {
    val downstreamFruitful: Stage[Int, Int, Nothing] = plain ~> new Echo[Int]
    val upstreamFruitful: Stage[Int, Int, Nothing] = new Echo[Int] ~> plain
    downstreamFruitful shouldBe a[Stage.AndThen[?, ?, ?, ?]]
    upstreamFruitful shouldBe a[Stage.AndThen[?, ?, ?, ?]]
  }

  "a fruitful composition" should "stay fruitful in the next generation" in {
    val composed: Stage.Fruitful[Int, Int, Nothing] = new Echo[Int] ~> new Echo[Int]
    val yld: Yield.Some.Fruitful[Int, Int, Nothing] = composed(1)
    yld.out shouldBe 1
    val next: Stage.Fruitful[Int, Int, Nothing] = yld.evolve()
    next(2).out shouldBe 2
  }

  it should "compose the skipped evolutions into a fruitful one" in {
    val composed: Stage.Fruitful[Int, Int, Nothing] = new Echo[Int] ~> new Echo[Int]
    val skipped: Evolution.Fruitful[Int, Int, Nothing] = composed.skip()
    skipped.evolve(Status.Success)(3).out shouldBe 3
  }

  "mapToFruitful" should "make a fruitful evolution out of an unclassified one" in {
    val unclassified: Evolution[Int, Int, Nothing] = new Echo[Int]
    val mapped: Evolution.Fruitful[Int, Int, Nothing] = unclassified.mapToFruitful(_ => new Echo[Int])
    mapped.evolve(Status.Success)(4).out shouldBe 4
  }
}
