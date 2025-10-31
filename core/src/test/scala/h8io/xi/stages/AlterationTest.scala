package h8io.xi.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.*
import java.util.UUID

class AlterationTest extends AnyFlatSpec with Matchers with MockFactory {
  "∘" should "compose alterations" in {
    val outer = mock[Alteration[Stage[ZoneOffset, OffsetDateTime, Exception], Stage[UUID, Instant, Long]]]
    val inner = mock[Alteration[Stage[ZoneId, ZonedDateTime, Nothing], Stage[ZoneOffset, OffsetDateTime, Exception]]]
    outer ∘ inner shouldBe AlterationCompose(outer, inner)
  }

  "arrows" should "compose alterations" in {
    val outer = mock[Alteration[Stage[ZoneOffset, OffsetDateTime, Exception], Stage[UUID, Instant, Long]]]
    val inner = mock[Alteration[Stage[ZoneId, ZonedDateTime, Nothing], Stage[ZoneOffset, OffsetDateTime, Exception]]]
    outer ~> inner shouldBe outer ∘ inner
    outer <| inner shouldBe outer ∘ inner
    inner <~ outer shouldBe outer ∘ inner
  }

  "⋅ and <|" should "apply a alteration to a stage" in {
    val alteration = mock[Alteration[Stage[ZoneOffset, OffsetDateTime, Exception], Stage[UUID, Instant, Long]]]
    val stage = mock[Stage[ZoneOffset, OffsetDateTime, Exception]]
    val in = mock[Stage[UUID, Instant, Long]]
    (alteration.apply _).expects(stage).returns(in)
    alteration ⋅ stage shouldBe in
    (alteration.apply _).expects(stage).returns(in)
    alteration <| stage shouldBe in
  }

  "AlterationCompose" should "apply alterations to a stage sequentially" in {
    val outer = mock[Alteration[Stage[ZoneOffset, OffsetDateTime, Exception], Stage[UUID, Instant, Long]]]
    val inner = mock[Alteration[Stage[ZoneId, ZonedDateTime, Nothing], Stage[ZoneOffset, OffsetDateTime, Exception]]]
    val input = mock[Stage[ZoneId, ZonedDateTime, Nothing]]
    val medium = mock[Stage[ZoneOffset, OffsetDateTime, Exception]]
    val output = mock[Stage[UUID, Instant, Long]]
    inSequence {
      (inner.apply _).expects(input).returns(medium)
      (outer.apply _).expects(medium).returns(output)
    }
    AlterationCompose(outer, inner)(input) shouldBe output
  }
}
