package h8io.stages.std

import h8io.stages
import h8io.stages.base.BaseEvolution
import h8io.stages.{Stage, Status}

/** A terminal stage that never produces an output value.
  *
  * Every invocation of `apply` returns a pre-built `h8io.stages.Yield.None` with `h8io.stages.Status.Complete`,
  * immediately signaling that the pipeline is finished. The `h8io.stages.Yield` instance is cached as a `val` to avoid
  * allocation on each call.
  *
  * `DeadEnd` is the natural "end-of-stream" marker and is also returned by [[Countdown.apply]] when `n ≤ 0` and by
  * [[h8io.stages.operators.LocalSoftDeadline]] when the duration is non-positive.
  *
  * An optional `_dispose` hook can be supplied at construction to release resources when the stage is disposed; the
  * companion object [[DeadEnd]] uses a no-op hook.
  *
  * @param _dispose
  *   a thunk invoked by [[dispose]]; defaults to a no-op in the companion object
  */
sealed case class DeadEnd(_dispose: () => Unit)
    extends Stage[Any, Nothing, Nothing] with BaseEvolution[Any, Nothing, Nothing] {

  /** The single pre-allocated `h8io.stages.Yield.None` returned by every `apply` call. */
  final val Yield: stages.Yield.None[Any, Nothing, Nothing] =
    stages.Yield.None[Any, Nothing, Nothing](Status.Complete, this)

  override final def apply(in: Any): stages.Yield.None[Any, Nothing, Nothing] = Yield

  override final def dispose(): Unit = _dispose()
}

/** Default no-op [[DeadEnd]] instance.
  *
  * Use `DeadEnd` directly as a stage or call `DeadEnd(_dispose)` to create a variant with a custom disposal hook.
  */
object DeadEnd extends DeadEnd({ () => })
