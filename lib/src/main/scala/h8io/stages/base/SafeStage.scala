package h8io.stages.base

import h8io.stages.{Stage, Yield}

import scala.util.control.NonFatal

trait SafeStage[-I, +O, +E] extends Stage[I, O, E] {
  override final def apply(in: I): Yield[I, O, E] =
    try body(in)
    catch {
      case NonFatal(e) => recover(in, e)
    }

  def body(in: I): Yield[I, O, E]

  def recover(in: I, e: Throwable): Yield[I, O, E]
}
