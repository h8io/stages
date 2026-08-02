package h8io.stages

/** What a [[Stage]] returns for one input: an optional output, the [[Status]] of that run, and the [[Evolution]] that
  * picks the stage handling the next one.
  *
  * `Yield` is the internal value flowing from stage to stage, not the answer the pipeline gives to the outside world —
  * that is `h8io.stages.base.Outcome` in the lib module, produced once someone terminates the pipeline.
  *
  * There are two variants, [[Yield.Some]] and [[Yield.None]], according to whether an output was produced. Match on
  * them when the two cases are handled differently, on [[Yield]] itself when they are not.
  *
  * @tparam I
  *   the input type of the stages held by the [[evolution]] (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
sealed trait Yield[-I, +O, +E] {

  /** The output as an `Option` — for when only its presence matters. Prefer matching on [[Yield.Some]] / [[Yield.None]]
    * when the status or the evolution is needed too.
    */
  def outOption: Option[O]

  /** The status of the run that produced this yield. */
  val status: Status[E]

  /** The continuation: which stage handles the next input, given [[status]]. */
  val evolution: Evolution[I, O, E]

  /** Transforms all three components at once. `mapOut` is not called on a [[Yield.None]], which has no output to
    * transform, but is still required so that the result type is the same for both variants.
    */
  def map[_I, _O, _E](
      mapOut: O => _O,
      mapStatus: Status[E] => Status[_E],
      mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E]

  /** The stage that handles the next input: the continuation this yield's own [[status]] selects.
    *
    * Going through the evolution directly is the exception, not the rule — the status a yield carries is the one its
    * continuation must be chosen by, and this method is what pairs them.
    */
  def evolve(): Stage[I, O, E] = evolution.evolve(status)
}

object Yield {

  /** A yield carrying an output.
    *
    * In a pipeline the output becomes the downstream stage's input, and the two yields are merged into one.
    *
    * @param out
    *   the output value
    * @param status
    *   the status of the run that produced this yield
    * @param evolution
    *   the continuation
    * @tparam I
    *   the input type of the stages held by the evolution (contravariant)
    * @tparam O
    *   the output type (covariant)
    * @tparam E
    *   the error type (covariant)
    */
  final case class Some[-I, +O, +E](out: O, status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {

    def outOption: Option[O] = scala.Some(out)

    /** Merges this yield with the one the downstream stage produced for [[out]]: statuses combine, evolutions compose,
      * and the output is the downstream's — or absent, if the downstream produced none.
      */
    private[stages] def compose[_O, _E >: E](that: Yield[O, _O, _E]): Yield[I, _O, _E] =
      that match {
        case Yield.Some(out, status, evolution) =>
          Yield.Some(out, this.status.combine(status), this.evolution.compose(evolution))
        case Yield.None(status, evolution) => Yield.None(this.status.combine(status), this.evolution.compose(evolution))
      }

    override def map[_I, _O, _E](
        mapOut: O => _O,
        mapStatus: Status[E] => Status[_E],
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield.Some[_I, _O, _E] =
      Yield.Some(mapOut(out), mapStatus(status), mapEvolution(evolution))
  }

  /** A yield carrying no output — a filter dropping an input, a stage with nothing to report yet. The status and the
    * evolution are still there, so the pipeline continues as usual.
    *
    * @param status
    *   the status of the run that produced this yield
    * @param evolution
    *   the continuation
    * @tparam I
    *   the input type of the stages held by the evolution (contravariant)
    * @tparam O
    *   the output type never produced here (covariant)
    * @tparam E
    *   the error type (covariant)
    */
  final case class None[-I, +O, +E](status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {

    override def outOption: Option[O] = scala.None

    /** Folds the skipped downstream into the evolution: with no output to feed it, the downstream cannot run now, so
      * the whole composition runs when an input eventually arrives.
      */
    private[stages] def compose[_O, _E >: E](downstream: Evolution[O, _O, _E]): Yield.None[I, _O, _E] =
      Yield.None(status, evolution.compose(downstream))

    override def map[_I, _O, _E](
        mapOut: O => _O,
        mapStatus: Status[E] => Status[_E],
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E] =
      Yield.None(mapStatus(status), mapEvolution(evolution))
  }

  /** Selects the variant by the optional output: `scala.Some` gives a [[Yield.Some]], `scala.None` a [[Yield.None]].
    * For when the presence of an output is only known at runtime.
    */
  def apply[I, O, E](out: Option[O], status: Status[E], evolution: Evolution[I, O, E]): Yield[I, O, E] =
    out match {
      case scala.Some(out) => Some(out, status, evolution)
      case scala.None => None(status, evolution)
    }

  /** Matches any yield as `Yield(out, status, evolution)` with the output optional — the inverse of [[apply]], for the
    * cases where both variants are treated uniformly. The `scala.Some` return type marks the extractor as irrefutable:
    * a single `case Yield(...)` is exhaustive.
    */
  def unapply[I, O, E](yld: Yield[I, O, E]): scala.Some[(Option[O], Status[E], Evolution[I, O, E])] =
    yld match {
      case Some(out, status, evolution) => scala.Some((scala.Some(out), status, evolution))
      case None(status, evolution) => scala.Some((scala.None, status, evolution))
    }
}
