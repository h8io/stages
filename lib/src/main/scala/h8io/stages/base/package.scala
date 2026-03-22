package h8io.stages

package object base {
  type UnaryOperator[+S <: Stage[I, ?, ?], -I, +O, +E] = Alterator[S, I, O, E]

  type Decorator[-I, +O, +E] = UnaryOperator[Stage[I, O, E], I, O, E]

  type Alteration[-IS <: Stage.Any, +OS <: Stage.Any] = IS => OS

  type Decoration[I, O, E] = Alteration[Stage[I, O, E], Stage[I, O, E]]
}
