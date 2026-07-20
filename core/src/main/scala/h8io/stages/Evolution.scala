package h8io.stages

import scala.util.control.NonFatal

/** A strategy that selects the next [[Stage]] to use when the pipeline is ready to re-process, based on the [[Status]]
  * carried by the [[Yield]] returned from the most recent stage application. In a composed pipeline, this refers to the
  * combined `Yield` produced for the current input.
  *
  * Every [[Yield]] carries an `Evolution` whose `evolve` method selects the next stage based on the current [[Status]].
  * The appropriate stage is selected based on the [[Yield.status]] — callers use `[[Yield.evolve]]` rather than
  * dispatching on the status directly.
  *
  * `Evolution` is contravariant in `I` and covariant in `O` and `E`, mirroring the variance of the [[Stage]] values it
  * returns.
  *
  * @tparam I
  *   the input type consumed by the returned stages (contravariant)
  * @tparam O
  *   the output type produced by the returned stages (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait Evolution[-I, +O, +E] {

  /** Returns the next [[Stage]] based on the given `status`.
    *
    * May release resources that are specific to this evolution instance and will not be reused by subsequent
    * generations (i.e. resources not needed by the returned stage or its own evolution).
    *
    * By default at most one of `evolve` and [[dispose]] is called on any evolution instance: a terminal driver (such as
    * `execute` in the lib module) only disposes, and a pipeline that continues only evolves, dropping the previous
    * evolution. The two calls are not mutually exclusive, though: a caller that has obtained the continuation via
    * `evolve` may still call [[dispose]] on the same instance later. Operators that own their inner stage (e.g. `Loop`
    * and `Repeat` in the lib module) do exactly that — they evolve the inner evolution eagerly and keep its `dispose`
    * as the terminal cleanup handle for the generation just constructed.
    *
    * @param status
    *   the status that determines the continuation stage
    */
  def evolve(status: Status[?]): Stage[I, O, E]

  /** Releases all resources held by the [[Stage]] that produced this evolution.
    *
    * After this call the producing stage must be considered permanently unusable — it must not be applied or skipped
    * again. This is the exclusive cleanup point for resources owned by the producing stage.
    *
    * Called when the producing stage is permanently shut down: whoever terminates a pipeline must dispose the evolution
    * of the final [[Yield]], so the continuation is released immediately rather than carried forward. The reference
    * terminal driver is the `execute` extension method in the lib module (`h8io.stages.base`).
    *
    * `dispose()` must stay valid after [[evolve]] has been called on the same instance, and must then release
    * everything still alive — including resources acquired while constructing the continuation. `evolve` transfers no
    * ownership: this evolution remains the cleanup point for its lineage until the continuation has run and produced an
    * evolution of its own, which takes over as the terminal handle from that moment on.
    *
    * Exception handling is not part of the core model: a stage whose `apply` throws produces no [[Yield]] and therefore
    * no `Evolution` — there is nothing the caller could dispose. Such a stage must release its own resources before
    * letting the exception escape (see the ''Lifecycle'' section in [[Stage]]).
    *
    * Implementations that hold no external resources may leave this as a no-op.
    */
  def dispose(): Unit

  /** Composes this evolution with another, creating a new evolution whose continuation for any status is the sequential
    * composition of the corresponding continuations of both evolutions.
    *
    * Specifically:
    * {{{
    *   composed(s) == self(s) ~> that(s)
    * }}}
    *
    * Used internally when merging evolutions during [[Yield]] composition inside [[Stage.AndThen]].
    *
    * @param that
    *   the downstream evolution to compose with
    * @tparam _O
    *   the output type of the resulting stages
    * @tparam _E
    *   the combined error type
    * @return
    *   a new evolution representing `self` followed by `that`
    */
  @inline final def compose[_O, _E >: E](that: Evolution[O, _O, _E]): Evolution[I, _O, _E] =
    Evolution.AndThen(that, this)

  /** Transforms every continuation of this evolution by applying `f` to the stage it returns.
    *
    * This is the public API for adapting an `Evolution` to a different stage type without exposing internal composition
    * details.
    *
    * @param f
    *   a function that transforms each continuation stage
    * @tparam _I
    *   the input type of the resulting stages
    * @tparam _O
    *   the output type of the resulting stages
    * @tparam _E
    *   the error type of the resulting stages
    * @return
    *   a new evolution with all continuations mapped by `f`
    */
  @inline final def map[_I, _O, _E](f: Stage[I, O, E] => Stage[_I, _O, _E]): Evolution[_I, _O, _E] =
    Evolution.Mapped(this, f)
}

object Evolution {
  type Endo[T, +E] = Evolution[T, T, E]

  type Any = Evolution[?, ?, ?]

  /** Disposes the evolutions in argument order, ensuring every one is attempted.
    *
    * The first non-fatal exception becomes the primary one: the remaining evolutions are still disposed, any further
    * non-fatal exception is added as suppressed to the primary, and the primary is re-thrown at the end.
    *
    * This is the single disposal discipline for every evolution that owns several inner evolutions (e.g. [[AndThen]],
    * or `BaseBinaryOperator.Evolution` and `Reduce` in the lib module); the parameter list is deliberately positional —
    * each call site maps its own domain roles onto the disposal order.
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

  /** An [[Evolution]] composed of two sequential evolutions.
    *
    * ==Parameter naming==
    *
    * The field names are intentionally the reverse of [[Stage.AndThen]]. In `Stage.AndThen`, `upstream` processes
    * `I → OI` and `downstream` processes `OI → O`. Here it is the other way around:
    *   - `downstream: Evolution[I, OI, E]` — holds the evolution of the pipeline's upstream stage (`I → OI`).
    *   - `upstream: Evolution[OI, O, E]` — holds the evolution of the pipeline's downstream stage (`OI → O`).
    *
    * The inversion follows from how `<~` routes data: `upstream(s) <~ downstream(s)` feeds `downstream`'s stage first,
    * so the `I → OI` evolution occupies `downstream` and `OI → O` occupies `upstream`. `compose` stores its receiver as
    * `downstream` and its argument as `upstream`:
    * {{{
    *   pipelineUpstream.skip().compose(pipelineDownstream.skip())
    *   // == Evolution.AndThen(upstream = pipelineDownstream.skip(),
    *   //                      downstream = pipelineUpstream.skip())
    * }}}
    *
    * ==Continuation composition==
    *
    * For any status `s`:
    * {{{
    *   composed(s) == upstream(s) <~ downstream(s)
    *   // data flow: I → downstream's stage → OI → upstream's stage → O
    * }}}
    *
    * ==Disposal==
    *
    * Both evolutions are disposed in the order `upstream` then `downstream` (i.e. pipeline-downstream first, then
    * pipeline-upstream), matching the reverse-order convention used in [[Stage.AndThen]] before disposal was moved to
    * `Evolution`.
    *
    * @param upstream
    *   the evolution of the pipeline's downstream stage (`OI → O`)
    * @param downstream
    *   the evolution of the pipeline's upstream stage (`I → OI`)
    * @tparam I
    *   input type of the composed pipeline
    * @tparam OI
    *   intermediate type between the two stages
    * @tparam O
    *   output type of the composed pipeline
    * @tparam E
    *   error type
    */
  final case class AndThen[-I, OI, +O, +E](upstream: Evolution[OI, O, E], downstream: Evolution[I, OI, E])
      extends Evolution[I, O, E] {

    override def evolve(status: Status[?]): Stage[I, O, E] = upstream.evolve(status) <~ downstream.evolve(status)

    /** Releases resources held by both composed evolutions via [[Evolution.dispose]], disposing `upstream` first, then
      * `downstream`.
      */
    override def dispose(): Unit = Evolution.dispose(upstream, downstream)
  }

  /** An [[Evolution]] whose continuations are produced by applying `f` to the corresponding continuations of
    * `evolution`.
    *
    * Created by [[Evolution#map]]. Disposal is delegated to the wrapped `evolution`.
    *
    * @param evolution
    *   the inner evolution whose continuations are transformed
    * @param f
    *   the function applied to each continuation stage
    * @tparam II
    *   input type of the inner stages
    * @tparam IO
    *   output type of the inner stages
    * @tparam IE
    *   error type of the inner stages
    * @tparam OI
    *   input type of the resulting stages (contravariant)
    * @tparam OO
    *   output type of the resulting stages (covariant)
    * @tparam OE
    *   error type of the resulting stages (covariant)
    */
  final case class Mapped[II, IO, IE, -OI, +OO, +OE](
      evolution: Evolution[II, IO, IE],
      f: Stage[II, IO, IE] => Stage[OI, OO, OE])
      extends Evolution[OI, OO, OE] {
    override def evolve(status: Status[?]): Stage[OI, OO, OE] = f(evolution.evolve(status))
    override def dispose(): Unit = evolution.dispose()
  }
}
