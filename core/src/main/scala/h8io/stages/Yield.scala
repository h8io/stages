package h8io.stages

sealed trait Yield[-I, +O, +E] {
  val status: Status[E]
  val evolution: Evolution[I, O, E]

  private[stages] def mapEvolution[_I, _O >: O, _E](
      status: Status[_E],
      f: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E]

  private[stages] def mapEvolution[_I, _O >: O, _E >: E](
      f: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E]

  private[stages] final def mapEvolutionAndBreak[_I, _O >: O, _E >: E](
      f: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E] = mapEvolution(status.break, f)
}

object Yield {
  final case class Some[-I, +O, +E](out: O, status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](that: Yield[O, _O, _E]): Yield[I, _O, _E] =
      that match {
        case Yield.Some(out, status, evolution) =>
          Yield.Some(out, this.status ++ status, this.evolution.compose(evolution))
        case Yield.None(status, evolution) => Yield.None(this.status ++ status, this.evolution.compose(evolution))
      }

    private[stages] def mapEvolution[_I, _O >: O, _E](
        status: Status[_E],
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, status, mapEvolution(evolution))

    private[stages] def mapEvolution[_I, _O >: O, _E >: E](
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, status, mapEvolution(evolution))
  }

  final case class None[-I, +O, +E](status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](next: Stage[O, _O, _E]): Yield.None[I, _O, _E] =
      Yield.None(status, evolution.compose(next))

    private[stages] def mapEvolution[_I, _O >: O, _E](
        status: Status[_E],
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(status, mapEvolution(evolution))

    private[stages] def mapEvolution[_I, _O >: O, _E >: E](
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(status, mapEvolution(evolution))
  }
}
