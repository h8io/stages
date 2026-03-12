package h8io.stages

package object base {
  type Alteration[-IS <: Stage.Any, +OS <: Stage.Any] = IS => OS

  type Decoration[I, O, E] = Alteration[Stage[I, O, E], Stage[I, O, E]]

  implicit final class AlterationOps[-IS <: Stage.Any, +OS <: Stage.Any](val alteration: Alteration[IS, OS])
      extends AnyVal {
    @inline def ∘[_IS <: Stage.Any](that: Alteration[_IS, IS]): Alteration[_IS, OS] = that andThen alteration

    @inline def ~>[_IS <: Stage.Any](that: Alteration[_IS, IS]): Alteration[_IS, OS] = alteration ∘ that

    @inline def <|[_IS <: Stage.Any](that: Alteration[_IS, IS]): Alteration[_IS, OS] = alteration ∘ that

    @inline def <~[_OS <: Stage.Any](that: Alteration[OS, _OS]): Alteration[IS, _OS] = that ∘ alteration

    @inline def ⋅(stage: IS): OS = alteration(stage)

    @inline def <|(stage: IS): OS = this ⋅ stage
  }

  implicit final class StageOps[-I, +O, +E](stage: Stage[I, O, E]) {
    @inline final def ~>[S <: Stage.Any, _O, _E >: E](
        alteration: Alteration[S, Stage[O, _O, _E]]): Alteration[S, Stage[I, _O, _E]] =
      alterand => stage ~> alteration(alterand)

    @inline final def |>[S <: Stage.Any](alteration: Alteration[Stage[I, O, E], S]): S = alteration ⋅ stage

    @inline final def alteration[_O, _E >: E]: Alteration[Stage[O, _O, _E], Stage[I, _O, _E]] = leftAlteration[_O, _E]

    @inline final def leftAlteration[_O, _E >: E]: Alteration[Stage[O, _O, _E], Stage[I, _O, _E]] = stage.~>[_O, _E]

    @inline final def rightAlteration[_I, _E >: E]: Alteration[Stage[_I, I, _E], Stage[_I, O, _E]] = stage.<~[_I, _E]
  }

  type Decorator[I, O, E] = Alterator[I, O, E, I, O, E]

  type BaseDecorator[I, O, E] = BaseAlterator[I, O, E, I, O, E]
}
