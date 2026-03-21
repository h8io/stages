package h8io.stages

package object base {
  type UnaryOperator[+S <: Stage[I, ?, ?], -I, +O, +E] = Alterator[S, I, O, E]

  type BaseUnaryOperator[+S <: Stage[I, ?, ?], -I, +O, +E] = BaseAlterator[S, I, O, E]

  type Decorator[-I, +O, +E] = UnaryOperator[Stage[I, O, E], I, O, E]

  type BaseDecorator[-I, +O, +E] = BaseUnaryOperator[Stage[I, O, E], I, O, E]
}
