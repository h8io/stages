package h8io.stages.operators

import h8io.stages.base.Decorator
import h8io.stages.{Evolution, Stage, Status, Yield}

/** A decorator that stops the pipeline when the inner stage produces no output.
  *
  * When the inner stage yields `h8io.stages.Yield.None` with `h8io.stages.Status.Success`, `CompleteIfNone` upgrades
  * the status to `h8io.stages.Status.Complete`, signaling that processing should not continue. All other
  * `h8io.stages.Yield` variants are forwarded unchanged (with the evolution mapped to preserve the decorator).
  *
  * This is useful for stages that return `None` when their input stream is exhausted — wrapping them with
  * `CompleteIfNone` turns the absence of output into a clean pipeline termination.
  *
  * @param alterand
  *   the inner stage to observe
  * @tparam I
  *   the input type
  * @tparam O
  *   the output type
  * @tparam E
  *   the error type
  */
final case class CompleteIfNone[I, O, E](alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] =
    alterand(in) match {
      case Yield.None(Status.Success, evolution) => Yield.None(Status.complete, evolution.map(CompleteIfNone(_)))
      case other => other.map(identity, identity, _.map(CompleteIfNone(_)))
    }

  override def skip(): Evolution[I, O, E] = alterand.skip().map(CompleteIfNone(_))
}
