package h8io.stages

sealed trait Yield[-I, +O, +E] {
  val signal: Signal[E]
  val onDone: OnDone[I, O, E]

  private[stages] def mapOnDone[_I, _O >: O, _E](
      signal: Signal[_E],
      f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E]

  private[stages] def mapOnDone[_I, _O >: O, _E >: E](f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E]

  private[stages] final def mapOnDoneAndBreak[_I, _O >: O, _E >: E](
      f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E] = mapOnDone(signal.break, f)
}

object Yield {
  final case class Some[-I, +O, +E](out: O, signal: Signal[E], onDone: OnDone[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](that: Yield[O, _O, _E]): Yield[I, _O, _E] =
      that match {
        case Yield.Some(out, signal, onDone) =>
          Yield.Some(out, this.signal ++ signal, this.onDone.compose(onDone))
        case Yield.None(signal, onDone) => Yield.None(this.signal ++ signal, this.onDone.compose(onDone))
      }

    private[stages] def mapOnDone[_I, _O >: O, _E](
        signal: Signal[_E],
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, signal, mapOnDone(onDone))

    private[stages] def mapOnDone[_I, _O >: O, _E >: E](
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, signal, mapOnDone(onDone))
  }

  final case class None[-I, +O, +E](signal: Signal[E], onDone: OnDone[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](next: Stage[O, _O, _E]): Yield.None[I, _O, _E] =
      Yield.None(signal, onDone.compose(next))

    private[stages] def mapOnDone[_I, _O >: O, _E](
        signal: Signal[_E],
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(signal, mapOnDone(onDone))

    private[stages] def mapOnDone[_I, _O >: O, _E >: E](
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(signal, mapOnDone(onDone))
  }
}
