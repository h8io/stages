package h8io.stages.base

import h8io.stages.Stage

import scala.util.control.NonFatal

trait Alterator[+S <: Stage.Any, -I, +O, +E] extends Stage[I, O, E] {
  def alterand: S

  type DisposeContext

  private def suppressAndRethrow(primary: Throwable)(body: => Unit): Nothing = {
    try body
    catch {
      case NonFatal(secondary) => primary.addSuppressed(secondary)
    }
    throw primary
  }

  override final def dispose(): Unit = {
    val context =
      try beforeDispose()
      catch {
        case NonFatal(primary) => suppressAndRethrow(primary)(alterand.dispose())
      }
    try alterand.dispose()
    catch {
      case NonFatal(primary) => suppressAndRethrow(primary)(afterDispose(context))
    }
    afterDispose(context)
  }

  def beforeDispose(): DisposeContext
  def afterDispose(ctx: DisposeContext): Unit
}
