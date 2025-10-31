package h8io.xi.stages

@FunctionalInterface
trait Stage[-I, +O, +E] extends (I => Yield[I, O, E]) with OnDone[I, O, E] {
  self =>

  def apply(in: I): Yield[I, O, E]

  def dispose(): Unit = {}

  def skip: OnDone[I, O, E] =
    new OnDone[I, O, E] {
      def onSuccess(): Stage[I, O, E] = self
      def onComplete(): Stage[I, O, E] = self
      def onError(): Stage[I, O, E] = self
    }

  def onComplete(): Stage[I, O, E] = this
  def onSuccess(): Stage[I, O, E] = this
  def onError(): Stage[I, O, E] = this

  @inline final def outcome(in: I): Outcome[O, E] = this(in).outcome()

  @inline final def ~>[_O, _E >: E](that: Stage[O, _O, _E]): Stage[I, _O, _E] = Stage.AndThen(this, that)

  @inline final def <~[_I, _E >: E](that: Stage[_I, I, _E]): Stage[_I, O, _E] = that ~> this

  @inline final def ~>[S <: Stage.Any, _O, _E >: E](
      alteration: Alteration[S, Stage[O, _O, _E]]): Alteration[S, Stage[I, _O, _E]] = stage => this ~> alteration(stage)

  @inline final def |>[S <: Stage.Any](alteration: Alteration[Stage[I, O, E], S]): S = alteration ⋅ this

  @inline final def alteration[_O, _E >: E]: Alteration[Stage[O, _O, _E], Stage[I, _O, _E]] = leftAlteration[_O, _E]

  @inline final def leftAlteration[_O, _E >: E]: Alteration[Stage[O, _O, _E], Stage[I, _O, _E]] = ~>[_O, _E]

  @inline final def rightAlteration[_I, _E >: E]: Alteration[Stage[_I, I, _E], Stage[_I, O, _E]] = <~[_I, _E]
}

object Stage {
  type Endo[T, +E] = Stage[T, T, E]

  type Any = Stage[?, ?, ?]

  final case class AndThen[-I, OI, +O, +E](previous: Stage[I, OI, E], next: Stage[OI, O, E]) extends Stage[I, O, E] {
    def apply(in: I): Yield[I, O, E] =
      previous(in) match {
        case some @ Yield.Some(out, _, _) => some.compose(next(out))
        case none: Yield.None[I, OI, E] => none.compose(next.skip)
      }

    override def dispose(): Unit = {
      next.dispose()
      previous.dispose()
    }
  }
}
