package h8io.xi.stages

trait OnDone[-I, +O, +E] {
  self =>

  def onSuccess(): Stage[I, O, E]
  def onComplete(): Stage[I, O, E]
  def onError(): Stage[I, O, E]

  @inline private[stages] final def compose[_O, _E >: E](that: OnDone[O, _O, _E]): OnDone[I, _O, _E] =
    new OnDone[I, _O, _E] {
      def onSuccess(): Stage[I, _O, _E] = that.onSuccess() <~ self.onSuccess()
      def onComplete(): Stage[I, _O, _E] = that.onComplete() <~ self.onComplete()
      def onError(): Stage[I, _O, _E] = that.onError() <~ self.onError()
    }

  final def map[_I, _O, _E](f: Stage[I, O, E] => Stage[_I, _O, _E]): OnDone[_I, _O, _E] =
    new OnDone[_I, _O, _E] {
      def onSuccess(): Stage[_I, _O, _E] = f(self.onSuccess())
      def onComplete(): Stage[_I, _O, _E] = f(self.onComplete())
      def onError(): Stage[_I, _O, _E] = f(self.onError())
    }
}
