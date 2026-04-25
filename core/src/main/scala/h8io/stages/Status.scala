package h8io.stages

/** The execution status produced by a [[Stage]].
  *
  * A `Status` travels alongside the output (or absence of output) in a [[Yield]] and is accumulated as stages are
  * composed. There are three possible states:
  *
  *   - [[Status.Success]] — the stage completed normally and processing may continue.
  *   - [[Status.Complete]] — the pipeline has finished successfully; no further input should be processed.
  *   - [[Status.Error]] — one or more errors occurred; the pipeline should stop and report them.
  *
  * Statuses form a semigroup under `combine`, used when merging the results of composed stages: `Success` is the
  * identity element, `Complete` dominates `Success` but yields to `Error`, and `Error` dominates all.
  *
  * @tparam E
  *   the error type (covariant)
  */
sealed trait Status[+E] {

  /** Combines this status with `next`, returning the more severe of the two.
    *
    * Precedence (lowest to highest): `Success` < `Complete` < `Error`. When two `Error` statuses are combined their
    * error lists are concatenated.
    */
  def combine[_E >: E](next: Status[_E]): Status[_E]

  /** Selects the appropriate continuation stage from `evolution` based on this status. */
  private[stages] def apply[I, O, _E](evolution: Evolution[I, O, _E]): Stage[I, O, _E]

  /** Returns the "break" variant of this status, used to signal pipeline termination.
    *
    * For [[Status.Success]] this returns [[Status.Complete]]; for all break statuses it returns `this` unchanged.
    */
  private[stages] def break: Status[E]

  /** Transforms the error values contained in this status.
    *
    * @param f
    *   the mapping function applied to each error value
    * @tparam _E
    *   the resulting error type
    * @return
    *   a new `Status` with errors mapped by `f`
    */
  def map[_E](f: E => _E): Status[_E]
}

/** Companion object containing the three concrete status variants. */
object Status {

  /** Indicates that a [[Stage]] completed successfully and the pipeline may continue.
    *
    * `Success` is the identity element of status composition: `Success.combine(s) == s` for any `s`.
    */
  case object Success extends Status[Nothing] {
    def combine[E](next: Status[E]): Status[E] =
      next match {
        case Success => this
        case that => that
      }

    private[stages] def apply[I, O, _E](evolution: Evolution[I, O, _E]): Stage[I, O, _E] = evolution.onSuccess()

    private[stages] def break: Status[Nothing] = Complete

    override def map[_E](f: Nothing => _E): this.type = this
  }

  /** Marker trait for statuses that signal the pipeline should stop processing.
    *
    * Both [[Complete]] and [[Error]] extend `Break`. Once a break status is produced, the pipeline will not request
    * further input even if more is available.
    *
    * @tparam E
    *   the error type (covariant)
    */
  sealed trait Break[+E] extends Status[E] {
    private[stages] def break: Status[E] = this
  }

  /** Indicates that the pipeline has finished processing normally, without errors.
    *
    * `Complete` dominates [[Success]] but yields to [[Error]] in composition: `Complete.combine(Success) == Complete`
    * and `Complete.combine(e: Error) == e`.
    */
  case object Complete extends Break[Nothing] {
    def combine[E](next: Status[E]): Status[E] =
      next match {
        case Success | Complete => this
        case that => that
      }

    private[stages] def apply[I, O, _E](evolution: Evolution[I, O, _E]): Stage[I, O, _E] = evolution.onComplete()

    override def map[_E](f: Nothing => _E): this.type = this
  }

  /** Indicates that one or more errors occurred during stage execution.
    *
    * Errors are stored as a non-empty list with `head` holding the first (or primary) error and `tail` holding any
    * additional errors accumulated through composition.
    *
    * `Error` is the most severe status: it dominates both [[Success]] and [[Complete]]. Composing two `Error` values
    * concatenates their error lists, preserving left-to-right order with the left `Error`'s `head` remaining the first
    * element.
    *
    * Because `Error` extends `Iterable[E]`, individual errors can be iterated with a standard `for`-comprehension or
    * `foreach`.
    *
    * @param head
    *   the primary error value
    * @param tail
    *   additional error values accumulated through pipeline composition
    * @tparam E
    *   the error type (covariant)
    */
  final case class Error[+E](override val head: E, override val tail: List[E]) extends Break[E] with Iterable[E] {
    def combine[_E >: E](next: Status[_E]): Status[_E] =
      next match {
        case Success | Complete => this
        case Error(head, tail) => Error(this.head, this.tail ::: head :: tail)
      }

    private[stages] def apply[I, O, _E](evolution: Evolution[I, O, _E]): Stage[I, O, _E] = evolution.onError()

    /** Returns all error values as a `List`, with `head` at the front. */
    @inline override def toList: List[E] = head :: tail

    override def iterator: Iterator[E] = toList.iterator

    /** Always `false`; an `Error` always contains at least one error value. */
    override def isEmpty: Boolean = false

    override def map[_E](f: E => _E): Error[_E] = Error(f(head), tail.map(f))

    override def toString: String = mkString(getClass.getSimpleName + "(", ", ", ")")
  }

  /** Factory methods for [[Error]]. */
  object Error {

    /** Creates an [[Error]] with a single error value and an empty tail.
      *
      * @param head
      *   the single error value
      * @tparam E
      *   the error type
      * @return
      *   an `Error` containing only `head`
      */
    def apply[E](head: E): Error[E] = new Error(head, Nil)
  }
}
