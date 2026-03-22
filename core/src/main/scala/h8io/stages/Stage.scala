package h8io.stages

import scala.util.control.NonFatal

@FunctionalInterface
trait Stage[-I, +O, +E] extends (I => Yield[I, O, E]) {
  def apply(in: I): Yield[I, O, E]

  def dispose(): Unit = {}

  @inline final def execute(in: I): Outcome[O, E] = {
    val yld = this(in)
    yld.status(yld.evolution).dispose()
    yld match {
      case Yield.Some(out, status, _) => Outcome.Some(out, status)
      case Yield.None(status, _) => Outcome.None(status)
    }
  }

  @inline final def ~>[_O, _E >: E](that: Stage[O, _O, _E]): Stage[I, _O, _E] = Stage.AndThen(this, that)

  @inline private[stages] final def <~[_I, _E >: E](that: Stage[_I, I, _E]): Stage[_I, O, _E] = that ~> this
}

object Stage {
  type Endo[T, +E] = Stage[T, T, E]

  type Any = Stage[?, ?, ?]

  final case class AndThen[-I, OI, +O, +E](previous: Stage[I, OI, E], next: Stage[OI, O, E]) extends Stage[I, O, E] {
    override def apply(in: I): Yield[I, O, E] =
      previous(in) match {
        case some @ Yield.Some(out, _, _) => some.compose(next(out))
        case none: Yield.None[I, OI, E] => none.compose(next)
      }

    override def dispose(): Unit = {
      try next.dispose()
      catch {
        case NonFatal(primary) => try previous.dispose()
          catch {
            case NonFatal(secondary) =>
              primary.addSuppressed(secondary)
          } finally throw primary
      }
      previous.dispose()
    }
  }
}
