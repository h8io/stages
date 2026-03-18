package h8io.stages.base

import h8io.stages.Stage

import scala.util.control.NonFatal

trait BinaryStage[-I, +LO, +RO, +O, +E] extends Stage[I, O, E] {
  def left: Stage[I, LO, E]
  def right: Stage[I, RO, E]

  type DisposeContext

  private def suppress(primary: Throwable)(body: => Unit): Unit =
    try body
    catch {
      case NonFatal(secondary) => primary.addSuppressed(secondary)
    }

  override final def dispose(): Unit = {
    val context =
      try beforeDispose()
      catch {
        case NonFatal(primary) =>
          suppress(primary)(right.dispose())
          suppress(primary)(left.dispose())
          throw primary
      }
    try right.dispose()
    catch {
      case NonFatal(primary) =>
        suppress(primary)(left.dispose())
        suppress(primary)(afterDispose(context))
        throw primary
    }
    try left.dispose()
    catch {
      case NonFatal(primary) =>
        suppress(primary)(afterDispose(context))
        throw primary
    }
    afterDispose(context)
  }

  def beforeDispose(): DisposeContext
  def afterDispose(ctx: DisposeContext): Unit
}
