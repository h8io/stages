package h8io.stages

package object base {
  type UnaryOperator[+S <: Stage[I, ?, ?], -I, +O, +E] = Alterator[S, I, O, E]

  type Decorator[-I, +O, +E] = UnaryOperator[Stage[I, O, E], I, O, E]
}
