package io.h8.stages.test

import io.h8.stages.Stage

trait TestStage[-I, +O] extends Stage[I, O] {
  def repeat(n: Int)(implicit counter: Counter): Stage[I, O] = counter.repeat(n) ~> this
  def never(implicit counter: Counter): Stage[I, O] = counter.never ~> this
  def once(implicit counter: Counter): Stage[I, O] = counter.once ~> this
}
