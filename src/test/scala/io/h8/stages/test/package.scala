package io.h8.stages

package object test {
  implicit class StageExtension[-I, +O](stage: Stage[I, O]) {
    def repeat(n: Int)(implicit counter: Counter): Stage[I, O] = counter.repeat[I](n) ~> stage
    def never(implicit counter: Counter): Stage[I, O] = counter.never[I] ~> stage
    def once(implicit counter: Counter): Stage[I, O] = counter.once[I] ~> stage
  }
}
