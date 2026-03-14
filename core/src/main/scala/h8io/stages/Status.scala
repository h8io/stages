package h8io.stages

sealed trait Status[+E] {
  private[stages] def ++[_E >: E](next: Status[_E]): Status[_E]

  private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E]

  private[stages] def break: Status[E]
}

object Status {
  case object Success extends Status[Nothing] {
    private[stages] def ++[E](next: Status[E]): Status[E] =
      next match {
        case Success => this
        case that => that
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onSuccess()

    private[stages] def break: Status[Nothing] = Complete
  }

  sealed trait Break[+E] extends Status[E] {
    private[stages] def break: Status[E] = this
  }

  case object Complete extends Break[Nothing] {
    private[stages] def ++[E](next: Status[E]): Status[E] =
      next match {
        case Success | Complete => this
        case that => that
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onComplete()
  }

  final case class Error[+E](override val head: E, override val tail: List[E]) extends Break[E] with Iterable[E] {
    private[stages] def ++[_E >: E](next: Status[_E]): Status[_E] =
      next match {
        case Success | Complete => this
        case Error(head, tail) => Error(this.head, this.tail ::: head :: tail)
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onError()

    @inline override def toList: List[E] = head :: tail

    def iterator: Iterator[E] = toList.iterator

    override def isEmpty: Boolean = false
  }

  object Error {
    def apply[E](head: E): Error[E] = new Error(head, Nil)
  }
}
