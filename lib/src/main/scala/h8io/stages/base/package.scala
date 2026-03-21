package h8io.stages

package object base {
  type UnaryOperator[-I, +_O, +O, +_E, +E] = Alterator[Stage[I, _O, _E], I, O, E]

  type BaseUnaryOperator[-I, +_O, +O, +_E, +E] = BaseAlterator[Stage[I, _O, _E], I, O, E]

  type Decorator[-I, +O, +E] = UnaryOperator[I, O, O, E, E]

  type BaseDecorator[-I, +O, +E] = BaseUnaryOperator[I, O, O, E, E]
}
