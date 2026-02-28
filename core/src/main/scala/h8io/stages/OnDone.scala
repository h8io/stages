package h8io.stages

/** Continuation policy for a [[Stage]] based on its completion signal.
  *
  * A value of `OnDone` selects the next stage to run after success, completion, or error. It is typically produced by a
  * [[Yield]] and composed when stages are chained.
  *
  * @tparam I
  *   input type of the next stage
  * @tparam O
  *   output type of the next stage
  * @tparam E
  *   error/signal payload type of the next stage
  */
trait OnDone[-I, +O, +E] {
  self =>

  /** Next stage to run after a success signal. */
  def onSuccess(): Stage[I, O, E]

  /** Next stage to run after a completion (break) signal. */
  def onComplete(): Stage[I, O, E]

  /** Next stage to run after an error signal. */
  def onError(): Stage[I, O, E]

  /** Sequentially composes this policy with `that` by composing their stages.
    *
    * The resulting policy runs `that` after `self` for each signal path.
    */
  @inline private[stages] final def compose[_O, _E >: E](that: OnDone[O, _O, _E]): OnDone[I, _O, _E] =
    new OnDone[I, _O, _E] {
      def onSuccess(): Stage[I, _O, _E] = that.onSuccess() <~ self.onSuccess()
      def onComplete(): Stage[I, _O, _E] = that.onComplete() <~ self.onComplete()
      def onError(): Stage[I, _O, _E] = that.onError() <~ self.onError()
    }

  /** Maps all next stages using `f`.
    *
    * @param f
    *   stage transformer applied to each branch
    * @return
    *   transformed policy
    */
  final def map[_I, _O, _E](f: Stage[I, O, E] => Stage[_I, _O, _E]): OnDone[_I, _O, _E] =
    new OnDone[_I, _O, _E] {
      def onSuccess(): Stage[_I, _O, _E] = f(self.onSuccess())
      def onComplete(): Stage[_I, _O, _E] = f(self.onComplete())
      def onError(): Stage[_I, _O, _E] = f(self.onError())
    }
}
