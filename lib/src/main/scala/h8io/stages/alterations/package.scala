package h8io.stages

package object alterations {
  type Decorator[-I, +O, +E] = Alterator[Stage[I, O, E], I, O, E]
}
