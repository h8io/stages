package h8io.stages

trait Evolution[-I, +O, +E] {
  self =>

  def onSuccess(): Stage[I, O, E]
  def onComplete(): Stage[I, O, E]
  def onError(): Stage[I, O, E]

  @inline private[stages] final def compose[_O, _E >: E](that: Evolution[O, _O, _E]): Evolution[I, _O, _E] =
    new Evolution[I, _O, _E] {
      override def onSuccess(): Stage[I, _O, _E] = that.onSuccess() <~ self.onSuccess()
      override def onComplete(): Stage[I, _O, _E] = that.onComplete() <~ self.onComplete()
      override def onError(): Stage[I, _O, _E] = that.onError() <~ self.onError()
    }

  @inline private[stages] final def compose[_O, _E >: E](stage: Stage[O, _O, _E]): Evolution[I, _O, _E] =
    new Evolution[I, _O, _E] {
      override def onSuccess(): Stage[I, _O, _E] = self.onSuccess() ~> stage
      override def onComplete(): Stage[I, _O, _E] = self.onComplete() ~> stage
      override def onError(): Stage[I, _O, _E] = self.onError() ~> stage
    }

  final def map[_I, _O, _E](f: Stage[I, O, E] => Stage[_I, _O, _E]): Evolution[_I, _O, _E] =
    new Evolution[_I, _O, _E] {
      override def onSuccess(): Stage[_I, _O, _E] = f(self.onSuccess())
      override def onComplete(): Stage[_I, _O, _E] = f(self.onComplete())
      override def onError(): Stage[_I, _O, _E] = f(self.onError())
    }
}

object Evolution {
  final case class FromStage[I, O, E](stage: Stage[I, O, E]) extends Evolution[I, O, E] {
    override def onSuccess(): Stage[I, O, E] = stage
    override def onComplete(): Stage[I, O, E] = stage
    override def onError(): Stage[I, O, E] = stage
  }
}
