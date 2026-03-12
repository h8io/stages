package h8io.stages

package object base {
  type Decorator[-I, +O, +E] = Alterator[Stage[I, O, E], I, O, E]
}
