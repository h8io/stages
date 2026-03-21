package h8io.stages

sealed trait Yield[-I, +O, +E] {
  val status: Status[E]
  val evolution: Evolution[I, O, E]

  def map[_I, _O, _E](
      mapOut: O => _O,
      mapStatus: Status[E] => Status[_E],
      mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E]
}

object Yield {
  final case class Some[-I, +O, +E](out: O, status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](that: Yield[O, _O, _E]): Yield[I, _O, _E] =
      that match {
        case Yield.Some(out, status, evolution) =>
          Yield.Some(out, this.status ++ status, this.evolution.compose(evolution))
        case Yield.None(status, evolution) => Yield.None(this.status ++ status, this.evolution.compose(evolution))
      }

    override def map[_I, _O, _E](
        mapOut: O => _O,
        mapStatus: Status[E] => Status[_E],
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(mapOut(out), mapStatus(status), mapEvolution(evolution))
  }

  final case class None[-I, +O, +E](status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {
    private[stages] def compose[_O, _E >: E](next: Stage[O, _O, _E]): Yield.None[I, _O, _E] =
      Yield.None(status, evolution.compose(next))

    override def map[_I, _O, _E](
        mapOut: O => _O,
        mapStatus: Status[E] => Status[_E],
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E] =
      Yield.None(mapStatus(status), mapEvolution(evolution))
  }
}
