package h8io.stages

trait Stage[-I, +O, +E] extends (I => Yield[I, O, E]) {
  def apply(in: I): Yield[I, O, E]

  def skip(): OnDone[I, O, E]

  def dispose(): Unit

  @inline final def execute(in: I): Outcome[O, E] = this(in).outcome()

  @inline final def ~>[_O, _E >: E](that: Stage[O, _O, _E]): Stage[I, _O, _E] = Stage.AndThen(this, that)

  @inline final def <~[_I, _E >: E](that: Stage[_I, I, _E]): Stage[_I, O, _E] = that ~> this
}

object Stage {
  type Endo[T, +E] = Stage[T, T, E]

  type Any = Stage[?, ?, ?]

  final case class AndThen[-I, OI, +O, +E](previous: Stage[I, OI, E], next: Stage[OI, O, E]) extends Stage[I, O, E] {
    override def apply(in: I): Yield[I, O, E] =
      previous(in) match {
        case some @ Yield.Some(out, _, _) => some.compose(next(out))
        case none: Yield.None[I, OI, E] => none.compose(next.skip())
      }

    override def skip(): OnDone[I, O, E] = previous.skip().compose(next.skip())

    override def dispose(): Unit = {
      next.dispose()
      previous.dispose()
    }
  }
}
