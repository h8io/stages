package h8io.xi.stages

sealed trait Signal[+E] extends Iterable[E] {
  private[stages] def ++[_E >: E](next: Signal[_E]): Signal[_E]

  private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E]

  private[stages] def break: Signal[E]
}

object Signal {
  case object Success extends Signal[Nothing] {
    private[stages] def ++[E](next: Signal[E]): Signal[E] =
      next match {
        case Success => this
        case that => that
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onSuccess()

    private[stages] def break: Signal[Nothing] = Complete

    @inline override def toList: List[Nothing] = Nil

    override def iterator: Iterator[Nothing] = Iterator.empty

    override def isEmpty: Boolean = true
  }

  sealed trait Break[+E] extends Signal[E] {
    private[stages] def break: Signal[E] = this
  }

  case object Complete extends Break[Nothing] {
    private[stages] def ++[E](next: Signal[E]): Signal[E] =
      next match {
        case Success | Complete => this
        case that => that
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onComplete()

    @inline override def toList: List[Nothing] = Nil

    override def iterator: Iterator[Nothing] = Iterator.empty

    override def isEmpty: Boolean = true
  }

  final case class Error[+E](override val head: E, override val tail: List[E] = Nil) extends Break[E] {
    private[stages] def ++[_E >: E](next: Signal[_E]): Signal[_E] =
      next match {
        case Success | Complete => this
        case Error(head, tail) => Error(this.head, this.tail ::: head :: tail)
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onError()

    @inline override def toList: List[E] = head :: tail

    override def iterator: Iterator[E] = toList.iterator

    override def isEmpty: Boolean = false
  }
}
