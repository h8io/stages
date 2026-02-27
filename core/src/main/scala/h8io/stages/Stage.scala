package h8io.stages

/** A computation step that consumes `I` and produces a [[Yield]] with an optional `O`, accumulated [[Signal]], and
  * continuation policy [[OnDone]].
  *
  * A `Stage` is both a function `I => Yield[I, O, E]` and an `OnDone` handler, which enables pipeline composition
  * (`~>`, `<~`) and alteration application (`|>`).
  *
  * @tparam I
  *   input type (contravariant)
  * @tparam O
  *   output type (covariant)
  * @tparam E
  *   error/signal payload type (covariant)
  */
@FunctionalInterface
trait Stage[-I, +O, +E] extends (I => Yield[I, O, E]) with OnDone[I, O, E] {

  /** Executes the stage for a given input.
    *
    * @param in
    *   input value
    * @return
    *   [[Yield]] containing optional output, signal, and continuation
    */
  def apply(in: I): Yield[I, O, E]

  /** Releases any resources held by this stage.
    *
    * Default implementation is a no-op.
    */
  def dispose(): Unit = {}

  /** Next stage to run after a completion signal. */
  def onComplete(): Stage[I, O, E] = this

  /** Next stage to run after a success signal. */
  def onSuccess(): Stage[I, O, E] = this

  /** Next stage to run after an error signal. */
  def onError(): Stage[I, O, E] = this

  /** Evaluates the stage and converts its [[Yield]] into an [[Outcome]].
    *
    * @param in
    *   input value
    * @return
    *   outcome with signal and dispose callback
    */
  @inline final def outcome(in: I): Outcome[O, E] = this(in).outcome()

  /** Sequentially composes this stage with `that` (this then `that`).
    *
    * Usage:
    * {{{
    * val parse: Stage[String, Int, ParseError] = ???
    * val inc: Stage[Int, Int, Nothing] = ???
    * val pipeline: Stage[String, Int, ParseError] = parse ~> inc
    * }}}
    *
    * @param that
    *   next stage consuming this stage's output
    * @return
    *   composed stage
    */
  @inline final def ~>[_O, _E >: E](that: Stage[O, _O, _E]): Stage[I, _O, _E] = Stage.AndThen(this, that)

  /** Sequentially composes `that` with this stage (`that` then this).
    *
    * Usage:
    * {{{
    * val prev: Stage[Long, String, Err] = ???
    * val next: Stage[String, Double, Err] = ???
    * val pipeline: Stage[Long, Double, Err] = next <~ prev
    * }}}
    *
    * @param that
    *   previous stage producing this stage's input
    * @return
    *   composed stage
    */
  @inline final def <~[_I, _E >: E](that: Stage[_I, I, _E]): Stage[_I, O, _E] = that ~> this

  /** Lifts a right alteration to act on the composed stage.
    *
    * @param alteration
    *   alteration producing a stage for this stage's output
    * @return
    *   alteration that produces a stage for this stage's input
    */
  @inline final def ~>[S <: Stage.Any, _O, _E >: E](
      alteration: Alteration[S, Stage[O, _O, _E]]): Alteration[S, Stage[I, _O, _E]] = stage => this ~> alteration(stage)

  /** Applies an alteration to this stage.
    *
    * Usage:
    * {{{
    * val loop: Alteration[Stage[Int, Int, Nothing], Stage[Int, Int, Nothing]] = ???
    * val out: Stage[Int, Int, Nothing] = stage |> loop
    * }}}
    *
    * @param alteration
    *   alteration to apply
    * @return
    *   altered value
    */
  @inline final def |>[S <: Stage.Any](alteration: Alteration[Stage[I, O, E], S]): S = alteration ⋅ this

  /** Alias for [[leftAlteration]]. */
  @inline final def alteration[_O, _E >: E]: Alteration[Stage[O, _O, _E], Stage[I, _O, _E]] = leftAlteration[_O, _E]

  /** Left-associative alteration derived from `~>`. */
  @inline final def leftAlteration[_O, _E >: E]: Alteration[Stage[O, _O, _E], Stage[I, _O, _E]] = ~>[_O, _E]

  /** Right-associative alteration derived from `<~`. */
  @inline final def rightAlteration[_I, _E >: E]: Alteration[Stage[_I, I, _E], Stage[_I, O, _E]] = <~[_I, _E]
}

/** Stage utilities and common type aliases. */
object Stage {

  /** Endomorphism stage: input and output have the same type. */
  type Endo[T, +E] = Stage[T, T, E]

  /** Existential stage type. */
  type Any = Stage[?, ?, ?]

  /** Sequential composition of two stages. */
  final case class AndThen[-I, OI, +O, +E](previous: Stage[I, OI, E], next: Stage[OI, O, E]) extends Stage[I, O, E] {

    /** Executes `previous`, then feeds its output into `next` if available.
      *
      * @param in
      *   input value for the previous stage
      * @return
      *   composed yield with combined signal and continuation
      */
    def apply(in: I): Yield[I, O, E] =
      previous(in) match {
        case some @ Yield.Some(out, _, _) => some.compose(next(out))
        case none: Yield.None[I, OI, E] => none.compose(next)
      }

    /** Disposes `next` and then `previous`. */
    override def dispose(): Unit = {
      next.dispose()
      previous.dispose()
    }
  }
}
