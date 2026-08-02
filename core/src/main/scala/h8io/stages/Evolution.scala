package h8io.stages

import scala.util.control.NonFatal

/** The continuation of a [[Stage]]: which stage handles the next input, chosen by the [[Status]] of the run that just
  * finished. This is what makes a pipeline able to change as it runs — every [[Yield]] carries one.
  *
  * An `Evolution` is also the exclusive owner of cleanup for the stage that produced it: see [[dispose]].
  *
  * The status to select on is the one carried alongside the evolution in the same yield, which is why callers normally
  * go through [[Yield.evolve]] rather than dispatching on a status themselves. In a composed pipeline that status is
  * the combined one of the whole run, never a single stage's own.
  *
  * @tparam I
  *   the input type of the stages returned (contravariant)
  * @tparam O
  *   their output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait Evolution[-I, +O, +E] {

  /** The next stage, for the given status.
    *
    * May release what belongs to this generation alone — anything the returned stage and its own evolution will not
    * reuse.
    *
    * `evolve` and [[dispose]] are not mutually exclusive. Usually only one of them is called on an instance: a terminal
    * driver disposes, a continuing pipeline evolves and drops the previous evolution. But a caller may do both, and
    * operators owning their inner stage (`Loop`, `Repeat` and the rest of `h8io.stages.cycles` in the lib module) do
    * exactly that — they evolve eagerly and keep the inner `dispose` as the cleanup handle of the generation they just
    * built.
    */
  def evolve(status: Status[?]): Stage[I, O, E]

  /** Releases everything held by the [[Stage]] that produced this evolution. That stage is permanently unusable
    * afterwards — neither applied nor skipped again — and this is the only place its resources are released.
    *
    * Whoever terminates a pipeline is responsible for disposing the evolution of the final [[Yield]], so that the
    * continuation is released at once instead of being carried around. The reference terminal driver is the `execute`
    * extension method of `h8io.stages.base` in the lib module.
    *
    * `dispose()` stays valid after [[evolve]] has been called on the same instance, and must then release everything
    * still alive, including whatever building the continuation acquired. `evolve` hands over no ownership: this
    * evolution remains the cleanup point for its lineage until the continuation has run and produced an evolution of
    * its own, which takes the role over from that moment.
    *
    * Exceptions are not part of this contract: a stage whose `apply` throws produces no yield and therefore no
    * evolution, so there is nothing to dispose — it must clean up after itself (see the ''Lifecycle'' section in
    * [[Stage]]).
    *
    * A no-op is a fine implementation for a stage holding nothing external.
    */
  def dispose(): Unit

  /** Composes this evolution with the one downstream of it, so that for every status
    * {{{
    *   composed(s) == self(s) ~> that(s)
    * }}}
    *
    * This is how [[Yield]] composition carries both continuations of a pipeline forward; application code rarely calls
    * it directly.
    */
  @inline final def compose[_O, _E >: E](that: Evolution[O, _O, _E]): Evolution[I, _O, _E] =
    Evolution.AndThen(that, this)

  /** Applies `f` to every stage this evolution would return, leaving disposal delegated to it unchanged.
    *
    * The way an operator keeps its wrapping in place across generations: the continuation comes back wrapped the same
    * way the current stage is.
    */
  @inline final def map[_I, _O, _E](f: Stage[I, O, E] => Stage[_I, _O, _E]): Evolution[_I, _O, _E] =
    Evolution.Mapped(this, f)
}

object Evolution {

  /** An evolution over stages whose input and output types coincide. */
  type Endo[T, +E] = Evolution[T, T, E]

  /** Any evolution, whatever its type parameters — the type [[Evolution.dispose]] collects. */
  type Any = Evolution[?, ?, ?]

  /** Disposes every evolution given, in argument order, whatever any of them throws.
    *
    * The first non-fatal exception becomes the primary one: the rest are disposed all the same, further non-fatal
    * exceptions are attached to the primary as suppressed, and the primary is rethrown at the end.
    *
    * This is the single disposal discipline for everything owning several evolutions at once — the composed evolution
    * here, the binary operators and the `h8io.stages.cycles` family in the lib module. The parameter list is positional
    * on purpose: each caller maps its own roles onto the order it wants them released in.
    *
    * @param evolutions
    *   the evolutions to dispose, in order, even if earlier disposals fail
    */
  def dispose(evolutions: Evolution.Any*): Unit =
    evolutions.foldLeft(Option.empty[Throwable]) { (primary, evolution) =>
      try {
        evolution.dispose()
        primary
      } catch {
        case NonFatal(e) =>
          primary match {
            case Some(p) =>
              p.addSuppressed(e)
              primary
            case None => Some(e)
          }
      }
    }.foreach(throw _)

  /** The continuation of a composed pipeline, built by [[Evolution.compose]]. Internal to the library, like the
    * `Stage.AndThen` it continues.
    *
    * ==Field names==
    *
    * They are deliberately the reverse of `Stage.AndThen`'s. There, `upstream` processes `I → OI` and `downstream`
    * `OI → O`; here it is the other way round:
    *
    *   - `downstream: Evolution[I, OI, E]` — the continuation of the pipeline's ''upstream'' stage;
    *   - `upstream: Evolution[OI, O, E]` — the continuation of the pipeline's ''downstream'' stage.
    *
    * The inversion follows the data flow of `Stage.<~`, which feeds its argument first, and `compose` storing its
    * receiver as `downstream`:
    * {{{
    *   pipelineUpstream.skip().compose(pipelineDownstream.skip())
    *   // == Evolution.AndThen(upstream = pipelineDownstream.skip(), downstream = pipelineUpstream.skip())
    * }}}
    *
    * ==Order==
    *
    * Both `evolve` and `dispose()` run the pipeline-downstream side first, then the pipeline-upstream one — the reverse
    * of the order the stages were applied in, so that a downstream stage can still reach whatever the upstream provides
    * until the moment the upstream is torn down.
    *
    * @param upstream
    *   the continuation of the pipeline's downstream stage (`OI → O`)
    * @param downstream
    *   the continuation of the pipeline's upstream stage (`I → OI`)
    * @tparam I
    *   the input type of the composed pipeline
    * @tparam OI
    *   the intermediate type between the two stages
    * @tparam O
    *   the output type of the composed pipeline
    * @tparam E
    *   the error type
    */
  private[stages] final case class AndThen[-I, OI, +O, +E](
      upstream: Evolution[OI, O, E],
      downstream: Evolution[I, OI, E])
      extends Evolution[I, O, E] {

    override def evolve(status: Status[?]): Stage[I, O, E] = upstream.evolve(status) <~ downstream.evolve(status)

    override def dispose(): Unit = Evolution.dispose(upstream, downstream)
  }

  /** The evolution built by [[Evolution.map]]: every continuation goes through `f`, disposal goes to the wrapped
    * evolution untouched. Internal to the library.
    *
    * @param evolution
    *   the inner evolution whose continuations are transformed
    * @param f
    *   the function applied to each continuation stage
    * @tparam II
    *   the input type of the inner stages
    * @tparam IO
    *   the output type of the inner stages
    * @tparam IE
    *   the error type of the inner stages
    * @tparam OI
    *   the input type of the resulting stages (contravariant)
    * @tparam OO
    *   the output type of the resulting stages (covariant)
    * @tparam OE
    *   the error type of the resulting stages (covariant)
    */
  private[stages] final case class Mapped[II, IO, IE, -OI, +OO, +OE](
      evolution: Evolution[II, IO, IE],
      f: Stage[II, IO, IE] => Stage[OI, OO, OE])
      extends Evolution[OI, OO, OE] {
    override def evolve(status: Status[?]): Stage[OI, OO, OE] = f(evolution.evolve(status))
    override def dispose(): Unit = evolution.dispose()
  }
}
