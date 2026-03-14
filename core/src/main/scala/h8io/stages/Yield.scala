package h8io.stages

sealed trait Yield[-I, +O, +E] {
  val status: Status[E]
  val onDone: OnDone[I, O, E]

  private[stages] def mapOnDone[_I, _O >: O, _E](
      status: Status[_E],
      f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E]

  private[stages] def mapOnDone[_I, _O >: O, _E >: E](f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E]

  private[stages] final def mapOnDoneAndBreak[_I, _O >: O, _E >: E](
      f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E] = mapOnDone(status.break, f)
}

object Yield {
  final case class Some[-I, +O, +E](out: O, status: Status[E], onDone: OnDone[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](that: Yield[O, _O, _E]): Yield[I, _O, _E] =
      that match {
        case Yield.Some(out, status, onDone) =>
          Yield.Some(out, this.status ++ status, this.onDone.compose(onDone))
        case Yield.None(status, onDone) => Yield.None(this.status ++ status, this.onDone.compose(onDone))
      }

    private[stages] def mapOnDone[_I, _O >: O, _E](
        status: Status[_E],
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, status, mapOnDone(onDone))

    private[stages] def mapOnDone[_I, _O >: O, _E >: E](
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, status, mapOnDone(onDone))
  }

  final case class None[-I, +O, +E](status: Status[E], onDone: OnDone[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](next: Stage[O, _O, _E]): Yield.None[I, _O, _E] =
      Yield.None(status, onDone.compose(next))

    private[stages] def mapOnDone[_I, _O >: O, _E](
        status: Status[_E],
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(status, mapOnDone(onDone))

    private[stages] def mapOnDone[_I, _O >: O, _E >: E](
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(status, mapOnDone(onDone))
  }
}
