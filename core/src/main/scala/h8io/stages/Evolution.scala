package h8io.stages

import scala.util.control.NonFatal

/** A strategy that selects the next [[Stage]] to use when the pipeline is ready to re-process, based on the [[Status]]
  * carried by the [[Yield]] returned from the most recent stage application. In a composed pipeline, this refers to the
  * combined `Yield` produced for the current input.
  *
  * Every [[Yield]] carries an `Evolution` that has three branches:
  *   - [[onSuccess]] — invoked when the previous stage succeeded ([[Status.Success]]).
  *   - [[onComplete]] — invoked when the pipeline signalled normal completion ([[Status.Complete]]).
  *   - [[onError]] — invoked when one or more errors were accumulated ([[Status.Error]]).
  *
  * The appropriate branch is selected based on the [[Yield.status]] — callers use `[[Yield.evolve]]` rather than
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
  self =>

  /** Returns the next [[Stage]] when the previous yield had [[Status.Success]].
    *
    * May release resources that are specific to this branch and will not be reused by subsequent generations of the
    * evolution (i.e. resources not needed by the returned stage or its own evolution).
    */
  def onSuccess(): Stage[I, O, E]

  /** Returns the next [[Stage]] when the previous yield had [[Status.Complete]].
    *
    * May release resources that are specific to this branch and will not be reused by subsequent generations of the
    * evolution (i.e. resources not needed by the returned stage or its own evolution).
    */
  def onComplete(): Stage[I, O, E]

  /** Returns the next [[Stage]] when the previous yield had [[Status.Error]].
    *
    * May release resources that are specific to this branch and will not be reused by subsequent generations of the
    * evolution (i.e. resources not needed by the returned stage or its own evolution).
    */
  def onError(): Stage[I, O, E]

  /** Releases all resources held by the [[Stage]] that produced this evolution.
    *
    * After this call the producing stage must be considered permanently unusable — it must not be applied or skipped
    * again. This is the exclusive cleanup point for resources owned by the producing stage.
    *
    * Called when the producing stage is permanently shut down:
    *   - by [[Stage.execute]] after the pipeline has produced its terminal [[Outcome]], so the continuation is released
    *     immediately rather than carried forward;
    *   - when a status method ([[onSuccess]], [[onComplete]], [[onError]]) throws a [[Throwable]], since the stage can
    *     no longer be used and all its resources must still be released.
    *
    * Implementations that hold no external resources may leave this as a no-op.
    */
  def dispose(): Unit

  /** Composes this evolution with another, creating a new evolution whose branches are the sequential composition of
    * the corresponding branches of both evolutions.
    *
    * Specifically, for each status branch `b`:
    * {{{
    *   composed.b() == self.b() ~> that.b()
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
  @inline private[stages] final def compose[_O, _E >: E](that: Evolution[O, _O, _E]): Evolution[I, _O, _E] =
    Evolution.AndThen(that, this)

  /** Transforms every branch of this evolution by applying `f` to the stage it returns.
    *
    * This is the public API for adapting an `Evolution` to a different stage type without exposing internal composition
    * details.
    *
    * @param f
    *   a function that transforms each branch stage
    * @tparam _I
    *   the input type of the resulting stages
    * @tparam _O
    *   the output type of the resulting stages
    * @tparam _E
    *   the error type of the resulting stages
    * @return
    *   a new evolution with all branches mapped by `f`
    */
  final def map[_I, _O, _E](f: Stage[I, O, E] => Stage[_I, _O, _E]): Evolution[_I, _O, _E] =
    new Evolution[_I, _O, _E] {
      override def onSuccess(): Stage[_I, _O, _E] = f(self.onSuccess())
      override def onComplete(): Stage[_I, _O, _E] = f(self.onComplete())
      override def onError(): Stage[_I, _O, _E] = f(self.onError())
      override def dispose(): Unit = self.dispose()
    }
}

object Evolution {
  type Endo[T, +E] = Evolution[T, T, E]

  /** An [[Evolution]] composed of two sequential evolutions, produced by [[Evolution.compose]].
    *
    * ==Parameter naming==
    *
    * The field names are intentionally the reverse of [[Stage.AndThen]]. In `Stage.AndThen`, `upstream` processes
    * `I → OI` and `downstream` processes `OI → O`. Here it is the other way around:
    *   - `downstream: Evolution[I, OI, E]` — holds the evolution of the pipeline's upstream stage (`I → OI`).
    *   - `upstream: Evolution[OI, O, E]` — holds the evolution of the pipeline's downstream stage (`OI → O`).
    *
    * This inversion is a direct consequence of how [[Evolution.compose]] constructs the value:
    * {{{
    *   pipelineUpstream.skip().compose(pipelineDownstream.skip())
    *   // == Evolution.AndThen(upstream = pipelineDownstream.skip(),
    *   //                      downstream = pipelineUpstream.skip())
    * }}}
    *
    * ==Branch composition==
    *
    * Each branch combines the corresponding stages using `<~`:
    * {{{
    *   upstream.onSuccess() <~ downstream.onSuccess()
    *   // == downstream.onSuccess() ~> upstream.onSuccess()
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

    /** Returns the stage to use when the pipeline status is [[Status.Success]]. */
    override def onSuccess(): Stage[I, O, E] = upstream.onSuccess() <~ downstream.onSuccess()

    /** Returns the stage to use when the pipeline status is [[Status.Complete]]. */
    override def onComplete(): Stage[I, O, E] = upstream.onComplete() <~ downstream.onComplete()

    /** Returns the stage to use when the pipeline status is [[Status.Error]]. */
    override def onError(): Stage[I, O, E] = upstream.onError() <~ downstream.onError()

    /** Releases resources held by both composed evolutions, disposing `upstream` first, then `downstream`.
      *
      * If `upstream.dispose()` throws a non-fatal exception, `downstream.dispose()` is still attempted. Any non-fatal
      * exception from `downstream.dispose()` is added as suppressed to the primary before it is re-thrown.
      */
    override def dispose(): Unit = {
      try upstream.dispose()
      catch {
        case NonFatal(primary) =>
          try downstream.dispose()
          catch { case NonFatal(secondary) => primary.addSuppressed(secondary) }
          finally throw primary
      }
      downstream.dispose()
    }
  }
}
