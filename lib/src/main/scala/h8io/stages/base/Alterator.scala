package h8io.stages.base

import h8io.stages.{OnDone, Stage}

import scala.util.control.NonFatal

trait Alterator[_I, _O, _E, -I, +O, +E] extends Stage[I, O, E] {
  protected def alterand: Stage[_I, _O, _E]

  type SkipContext

  final def skip(): OnDone[I, O, E] = afterSkip(beforeSkip(), alterand.skip())

  protected def beforeSkip(): SkipContext
  protected def afterSkip(context: SkipContext, onDone: OnDone[_I, _O, _E]): OnDone[I, O, E]

  type DisposeContext

  private def suppressAndRethrow(primary: Throwable)(body: => Unit): Nothing = {
    try body
    catch {
      case NonFatal(secondary) => primary.addSuppressed(secondary)
    }
    throw primary
  }

  final def dispose(): Unit = {
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

  protected def beforeDispose(): DisposeContext
  protected def afterDispose(context: DisposeContext): Unit
}
