package h8io.stages.base

import h8io.stages.{OnDone, Stage}

trait BaseBinOp[I, LO, RO, +O, E] extends BinOp[I, LO, RO, O, E] {
  override val left: Stage[I, LO, E]
  override val right: Stage[I, RO, E]

  protected def withOperands(left: Stage[I, LO, E], right: Stage[I, RO, E]): Stage[I, O, E]

  override def skip: OnDone[I, O, E] = {
    val leftOnDone = left.skip
    val rightOnDone = right.skip
    new OnDone[I, O, E] {
      override def onSuccess(): Stage[I, O, E] = {
        val right = rightOnDone.onSuccess()
        val left = leftOnDone.onSuccess()
        withOperands(left, right)
      }

      override def onComplete(): Stage[I, O, E] = {
        val right = rightOnDone.onComplete()
        val left = leftOnDone.onComplete()
        withOperands(left, right)
      }

      override def onError(): Stage[I, O, E] = {
        val right = rightOnDone.onError()
        val left = leftOnDone.onError()
        withOperands(left, right)
      }
    }
  }

  override def dispose(): Unit = {
    right.dispose()
    left.dispose()
  }
}
