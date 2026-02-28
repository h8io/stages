package h8io.stages

/** Result of a [[Stage]] evaluation.
  *
  * A `Yield` carries an accumulated [[Signal]] and a continuation policy [[OnDone]]. Concrete variants decide whether
  * an output value is present.
  *
  * @tparam I
  *   input type for the continuation policy
  * @tparam O
  *   output type (present only in [[Yield.Some]])
  * @tparam E
  *   error/signal payload type
  */
sealed trait Yield[-I, +O, +E] {

  /** Accumulated signal for this evaluation. */
  val signal: Signal[E]

  /** Continuation policy for the next stage. */
  val onDone: OnDone[I, O, E]

  /** Maps the continuation with a new signal and return types.
    *
    * This is used internally to retarget the continuation when composing yields.
    */
  private[stages] def mapOnDone[_I, _O >: O, _E](
      signal: Signal[_E],
      f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E]

  /** Maps the continuation and return types while preserving the signal. */
  private[stages] def mapOnDone[_I, _O >: O, _E >: E](f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E]

  /** Maps the continuation and forces a break signal. */
  private[stages] final def mapOnDoneAndBreak[_I, _O >: O, _E >: E](
      f: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield[_I, _O, _E] = mapOnDone(signal.break, f)

  /** Converts this yield to an [[Outcome]] with a dispose callback. */
  private[stages] def outcome(): Outcome[O, E]
}

object Yield {

  /** Yield with an output value. */
  final case class Some[-I, +O, +E](out: O, signal: Signal[E], onDone: OnDone[I, O, E]) extends Yield[I, O, E] {

    /** Composes this yield with the next yield, combining signals and policies. */
    private[stages] def compose[_O, _E >: E](that: Yield[O, _O, _E]): Yield[I, _O, _E] =
      that match {
        case Yield.Some(out, signal, onDone) =>
          Yield.Some(out, this.signal ++ signal, this.onDone.compose(onDone))
        case Yield.None(signal, onDone) => Yield.None(this.signal ++ signal, this.onDone.compose(onDone))
      }

    /** Maps the continuation with a new signal and return types. */
    private[stages] def mapOnDone[_I, _O >: O, _E](
        signal: Signal[_E],
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, signal, mapOnDone(onDone))

    /** Maps the continuation and return types while preserving the signal. */
    private[stages] def mapOnDone[_I, _O >: O, _E >: E](
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(out, signal, mapOnDone(onDone))

    /** Builds an [[Outcome]] with output, signal, and a dispose callback. */
    private[stages] def outcome(): Outcome.Some[O, E] = Outcome.Some(out, signal, signal(onDone).dispose _)
  }

  /** Yield without an output value. */
  final case class None[-I, +O, +E](signal: Signal[E], onDone: OnDone[I, O, E]) extends Yield[I, O, E] {

    /** Composes this yield with the next policy, combining signals and policies. */
    private[stages] def compose[_O, _E >: E](next: OnDone[O, _O, _E]): Yield.None[I, _O, _E] =
      Yield.None(signal, onDone.compose(next))

    /** Maps the continuation with a new signal and return types. */
    private[stages] def mapOnDone[_I, _O >: O, _E](
        signal: Signal[_E],
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(signal, mapOnDone(onDone))

    /** Maps the continuation and return types while preserving the signal. */
    private[stages] def mapOnDone[_I, _O >: O, _E >: E](
        mapOnDone: OnDone[I, O, E] => OnDone[_I, _O, _E]): Yield.None[_I, _O, _E] =
      Yield.None(signal, mapOnDone(onDone))

    /** Builds an [[Outcome]] with signal and a dispose callback. */
    private[stages] def outcome(): Outcome.None[E] = Outcome.None(signal, signal(onDone).dispose _)
  }
}
