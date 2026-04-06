package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.{BaseDecorator, BaseEvolution}

import scala.annotation.tailrec

/** A decorator that feeds the output of each successful step back as the input of the next, effectively looping the
  * inner endomorphic stage until it stops.
  *
  * The tail-recursive loop works as follows:
  *
  *   - `h8io.stages.Status.Success` + `h8io.stages.Yield.Some`: the output becomes the new input and the loop continues
  *     with the `onSuccess` continuation.
  *   - `h8io.stages.Status.Success` + `h8io.stages.Yield.None`: no output was produced; the loop stops and emits
  *     `Yield.None(Success, Loop(onComplete))`.
  *   - `h8io.stages.Status.Complete`: the loop stops, converts the status back to `Success`, and selects the
  *     `onComplete` continuation for the next outer invocation.
  *   - `h8io.stages.Status.Error`: the loop stops, preserves the error, and selects the `onError` continuation.
  *
  * This makes `Loop` suitable for in-process iterative computations (e.g., fixed-point iterations) where the result of
  * one step seeds the next.
  *
  * @param alterand
  *   the inner endomorphic stage `T → T` to loop
  * @tparam T
  *   the value type (both input and output)
  * @tparam E
  *   the error type (covariant)
  */
final case class Loop[T, +E](alterand: Stage.Endo[T, E]) extends BaseDecorator[T, T, E] with BaseEvolution.Endo[T, E] {
  override def apply(in: T): Yield[T, T, E] = {
    @tailrec def loop(stage: Stage[T, T, E], in: T): Yield[T, T, E] = {
      val yld = stage(in)
      yld.status match {
        case Status.Success =>
          yld match {
            case Yield.Some(out, _, _) => loop(yld.evolution.onSuccess(), out)
            case Yield.None(_, _) => Yield.None(Status.Success, Loop(yld.evolution.onComplete()))
          }
        case Status.Complete => yld.map(identity, _ => Status.Success, evolution => Loop(evolution.onComplete()))
        case _: Status.Error[E] => yld.map(identity, identity, evolution => Loop(evolution.onError()))
      }
    }
    loop(alterand, in)
  }

  override def skip(): Evolution[T, T, E] = alterand.skip().map(Loop(_))
}
