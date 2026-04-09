package h8io.stages

/** The immediate result returned by a [[Stage]] when applied to an input value.
  *
  * A `Yield` carries three pieces of information:
  *   - An optional output value (`O`), present only in [[Yield.Some]].
  *   - A [[Status]] representing the outcome of this stage invocation.
  *   - An [[Evolution]] that decides which stage to use when re-processing based on the status.
  *
  * The type parameters follow the same variance rules as [[Stage]]: contravariant in `I` (the input consumed by the
  * continuation stages stored in the evolution) and covariant in `O` and `E`.
  *
  * @tparam I
  *   the input type consumed by the [[Evolution]] stages (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
sealed trait Yield[-I, +O, +E] {

  /** The status produced by the stage that created this `Yield`. */
  val status: Status[E]

  /** The evolution strategy that determines which stage to invoke next, depending on [[status]]. */
  val evolution: Evolution[I, O, E]

  /** Returns a new `Yield` with each component independently transformed.
    *
    * @param mapOut
    *   maps the output value (only applied in [[Yield.Some]])
    * @param mapStatus
    *   maps the status
    * @param mapEvolution
    *   maps the evolution
    * @tparam _I
    *   the new input type for the evolution
    * @tparam _O
    *   the new output type
    * @tparam _E
    *   the new error type
    * @return
    *   a transformed `Yield`
    */
  def map[_I, _O, _E](
      mapOut: O => _O,
      mapStatus: Status[E] => Status[_E],
      mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E]

  /** Resolves the next [[Stage]] by applying the current [[status]] to the [[evolution]].
    *
    * This is the primary way to advance a pipeline after processing an input: the status selects the appropriate branch
    * of the evolution (success, complete, or error), returning the stage that should handle the next input.
    *
    * @return
    *   the next `Stage` to use for re-processing, as determined by `status(evolution)`
    */
  final def evolve(): Stage[I, O, E] = status(evolution)
}

/** Companion object containing the two concrete variants of [[Yield]]. */
object Yield {

  /** A [[Yield]] that carries an output value produced by the stage.
    *
    * When composing two stages (see [[Stage.AndThen]]), if the first stage yields a `Some`, its output is passed as
    * input to the second stage and the two results are merged via `compose`.
    *
    * @param out
    *   the output value produced by the stage
    * @param status
    *   the execution status
    * @param evolution
    *   the evolution strategy for re-processing
    * @tparam I
    *   the input type for the evolution stages (contravariant)
    * @tparam O
    *   the output type (covariant)
    * @tparam E
    *   the error type (covariant)
    */
  final case class Some[-I, +O, +E](out: O, status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {

    /** Merges this `Some` with the [[Yield]] produced by the next stage in a pipeline.
      *
      * The statuses are combined with `++` and the evolutions are composed. The resulting `Yield` type depends on
      * whether `that` is a `Some` or `None`.
      *
      * @param that
      *   the `Yield` produced by the downstream stage
      * @tparam _O
      *   the output type of `that`
      * @tparam _E
      *   the combined error type
      * @return
      *   the merged `Yield`
      */
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

  /** A [[Yield]] that carries no output value.
    *
    * This can occur when a stage decides not to emit a value for the current input (e.g. a filter stage that drops
    * items). The pipeline still carries a [[Status]] and an [[Evolution]] so that processing can resume or terminate
    * correctly.
    *
    * When composing inside a [[Stage.AndThen]], a `None` from the first stage is forwarded without invoking the second
    * stage; instead, the second stage is wired into the evolution so that it will be applied when the pipeline
    * eventually resumes.
    *
    * @param status
    *   the execution status
    * @param evolution
    *   the evolution strategy for re-processing
    * @tparam I
    *   the input type for the evolution stages (contravariant)
    * @tparam O
    *   the (phantom) output type (covariant)
    * @tparam E
    *   the error type (covariant)
    */
  final case class None[-I, +O, +E](status: Status[E], evolution: Evolution[I, O, E]) extends Yield[I, O, E] {

    /** Composes this `None` with `next` by threading `next` through all branches of the evolution.
      *
      * Because no output was produced, `next` cannot be applied immediately; instead it becomes part of the evolution
      * so that the entire composed stage is invoked when the pipeline resumes.
      *
      * @param downstream
      *   the next stage in the pipeline
      * @tparam _O
      *   the output type of `next`
      * @tparam _E
      *   the combined error type
      * @return
      *   a `None` with the composed evolution
      */
    private[stages] def compose[_O, _E >: E](downstream: Evolution[O, _O, _E]): Yield.None[I, _O, _E] =
      Yield.None(status, evolution.compose(downstream))

    override def map[_I, _O, _E](
        mapOut: O => _O,
        mapStatus: Status[E] => Status[_E],
        mapEvolution: Evolution[I, O, E] => Evolution[_I, _O, _E]): Yield[_I, _O, _E] =
      Yield.None(mapStatus(status), mapEvolution(evolution))
  }
}
