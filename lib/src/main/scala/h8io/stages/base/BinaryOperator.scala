package h8io.stages.base

import h8io.stages.Stage

import scala.util.control.NonFatal

trait BinaryOperator[+LS <: Stage[I, ?, ?], +RS <: Stage[I, ?, ?], -I, +O, +E] extends Stage[I, O, E] {
  def left: LS
  def right: RS

  override final def dispose(): Unit = {
    try right.dispose()
    catch {
      case NonFatal(primary) =>
        try left.dispose()
        catch {
          case NonFatal(secondary) => primary.addSuppressed(secondary)
        } finally throw primary
    }
    left.dispose()
  }
}
