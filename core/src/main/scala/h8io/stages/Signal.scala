package h8io.stages

/** Completion signal produced by a stage evaluation.
  *
  * Signals can be combined when yields are composed. Certain signals are terminal (break) and stop further progression
  * in a pipeline.
  *
  * @tparam E
  *   error/signal payload type
  */
sealed trait Signal[+E] {

  /** Combines this signal with the next signal. */
  private[stages] def ++[_E >: E](next: Signal[_E]): Signal[_E]

  /** Selects the next stage using the provided continuation policy. */
  private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E]

  /** Converts this signal into a break signal if applicable. */
  private[stages] def break: Signal[E]
}

object Signal {

  /** Successful (non-breaking) signal. */
  case object Success extends Signal[Nothing] {
    private[stages] def ++[E](next: Signal[E]): Signal[E] =
      next match {
        case Success => this
        case that => that
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onSuccess()

    private[stages] def break: Signal[Nothing] = Complete
  }

  /** Base type for break signals that stop further progress in a pipeline. */
  sealed trait Break[+E] extends Signal[E] {
    private[stages] def break: Signal[E] = this
  }

  /** Completion (break) signal without error. */
  case object Complete extends Break[Nothing] {
    private[stages] def ++[E](next: Signal[E]): Signal[E] =
      next match {
        case Success | Complete => this
        case that => that
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onComplete()
  }

  /** Error (break) signal with a non-empty error list. */
  final case class Error[+E](override val head: E, override val tail: List[E]) extends Break[E] with Iterable[E] {
    private[stages] def ++[_E >: E](next: Signal[_E]): Signal[_E] =
      next match {
        case Success | Complete => this
        case Error(head, tail) => Error(this.head, this.tail ::: head :: tail)
      }

    private[stages] def apply[I, O, _E](onDone: OnDone[I, O, _E]): Stage[I, O, _E] = onDone.onError()

    /** Returns all errors in encounter order. */
    @inline override def toList: List[E] = head :: tail

    /** Iterates over all errors. */
    def iterator: Iterator[E] = toList.iterator

    /** Always false because `Error` is non-empty. */
    override def isEmpty: Boolean = false
  }

  object Error {

    /** Creates an error signal with a single error value. */
    def apply[E](head: E): Error[E] = new Error(head, Nil)
  }
}
