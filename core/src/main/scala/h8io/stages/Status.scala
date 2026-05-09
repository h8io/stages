package h8io.stages

import scala.annotation.nowarn

/** The execution status produced by a [[Stage]].
  *
  * A `Status` travels alongside the output (or absence of output) in a [[Yield]] and is accumulated as stages are
  * composed. There are two possible states:
  *
  *   - [[Status.Success]] — the stage completed normally and processing may continue.
  *   - [[Status.Complete]] — the pipeline has finished; no further input should be processed. When `errors` is empty
  *     this represents clean completion; when `errors` is non-empty it represents one or more accumulated errors.
  *
  * Statuses form a semigroup under `combine`, used when merging the results of composed stages: `Success` is the
  * identity element and `Complete` accumulates errors on combination.
  *
  * @tparam E
  *   the error type (covariant)
  */
sealed trait Status[+E] {

  /** Combines this status with `next`, returning the more severe of the two.
    *
    * `Success` is the identity element; `Complete` dominates `Success` and two `Complete` values are merged by
    * concatenating their error sequences.
    */
  def combine[_E >: E](next: Status[_E]): Status[_E]

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

/** Companion object containing the concrete status variants. */
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

    override def map[_E](f: Nothing => _E): this.type = this
  }

  /** Indicates that the pipeline has finished processing.
    *
    * When `errors` is empty (i.e., `this == complete`) the pipeline completed normally without errors. When `errors` is
    * non-empty the pipeline encountered one or more accumulated errors.
    *
    * `Complete` dominates [[Success]] in composition. Two `Complete` statuses are merged by concatenating their error
    * sequences.
    *
    * Because `Complete` extends `Iterable[E]`, individual errors can be iterated directly with a `for`-comprehension or
    * `foreach`.
    *
    * @param errors
    *   the accumulated error values; empty for a clean completion
    * @tparam E
    *   the error type (covariant)
    */
  final case class Complete[+E](errors: Seq[E]) extends Status[E] with Iterable[E] {
    def combine[_E >: E](next: Status[_E]): Status[_E] =
      next match {
        case Status.Success => this
        case Complete(errors) => Complete(this.errors ++ errors)
      }

    override def map[_E](f: E => _E): Complete[_E] = Complete(errors.map(f))

    override def iterator: Iterator[E] = errors.iterator

    override def isEmpty: Boolean = errors.isEmpty

    @nowarn("cat=deprecation")
    override def stringPrefix: String = "Complete"
  }

  /** A [[Complete]] with no errors, representing clean pipeline termination. */
  val complete: Complete[Nothing] = Complete(Nil)

  /** Creates a [[Complete]] status from one or more error values.
    *
    * @param head
    *   the first (required) error value
    * @param tail
    *   any additional error values
    */
  def error[E](head: E, tail: E*): Complete[E] = Complete(head +: tail)
}
