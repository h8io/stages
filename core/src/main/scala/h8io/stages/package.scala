package h8io

package object stages {

  /** Transformation from one stage type to another. */
  type Alteration[-IS <: Stage.Any, +OS <: Stage.Any] = IS => OS

  /** Alteration that preserves input/output/error types. */
  type Decoration[I, O, E] = Alteration[Stage[I, O, E], Stage[I, O, E]]

  /** Type alias for an alterator that decorates a stage. */
  type Decorator[-I, +O, +E] = Alterator[Stage[I, O, E], I, O, E]

  /** Syntax for composing and applying alterations. */
  implicit final class AlterationOps[-IS <: Stage.Any, +OS <: Stage.Any](val alteration: Alteration[IS, OS])
      extends AnyVal {

    /** Composes this alteration with `that`, applying `that` first. */
    @inline def ∘[_IS <: Stage.Any](that: Alteration[_IS, IS]): Alteration[_IS, OS] =
      AlterationCompose(alteration, that)

    /** Alias for composition (left to right). */
    @inline def ~>[_IS <: Stage.Any](that: Alteration[_IS, IS]): Alteration[_IS, OS] = alteration ∘ that

    /** Alias for composition (left to right). */
    @inline def <|[_IS <: Stage.Any](that: Alteration[_IS, IS]): Alteration[_IS, OS] = alteration ∘ that

    /** Composes in the reverse direction. */
    @inline def <~[_OS <: Stage.Any](that: Alteration[OS, _OS]): Alteration[IS, _OS] = that ∘ alteration

    /** Applies this alteration to a stage. */
    @inline def ⋅(stage: IS): OS = alteration(stage)

    /** Alias for applying an alteration to a stage. */
    @inline def <|(stage: IS): OS = this ⋅ stage
  }
}
