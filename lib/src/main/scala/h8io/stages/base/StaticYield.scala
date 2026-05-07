package h8io.stages.base

import h8io.stages.Status

sealed trait StaticYield[+O, +E] {
  def status: Status[E]
}

object StaticYield {
  final case class Some[+O, +E](out: O, status: Status[E]) extends StaticYield[O, E]

  final case class None[+E](status: Status[E]) extends StaticYield[Nothing, E]
}
