package h8io.stages

/** A strategy that selects the next [[Stage]] to use when the pipeline is ready to re-process, based on the [[Status]]
  * carried by the [[Yield]] returned from the most recent stage application. In a composed pipeline, this refers to the
  * combined `Yield` produced for the current input.
  *
  * Every [[Yield]] carries an `Evolution` that has three branches:
  *   - [[onSuccess]] — invoked when the previous stage succeeded ([[Status.Success]]).
  *   - [[onComplete]] — invoked when the pipeline signalled normal completion ([[Status.Complete]]).
  *   - [[onError]] — invoked when one or more errors were accumulated ([[Status.Error]]).
  *
  * The branch is selected by `Status.apply` inside [[Stage.execute]], so callers never need to dispatch on the status
  * themselves.
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

  /** Returns the stage to use when the previous yield had [[Status.Success]]. */
  def onSuccess(): Stage[I, O, E]

  /** Returns the stage to use when the previous yield had [[Status.Complete]]. */
  def onComplete(): Stage[I, O, E]

  /** Returns the stage to use when the previous yield had [[Status.Error]]. */
  def onError(): Stage[I, O, E]

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
    new Evolution[I, _O, _E] {
      override def onSuccess(): Stage[I, _O, _E] = that.onSuccess() <~ self.onSuccess()
      override def onComplete(): Stage[I, _O, _E] = that.onComplete() <~ self.onComplete()
      override def onError(): Stage[I, _O, _E] = that.onError() <~ self.onError()
    }

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
    }
}
