# LocalSoftDeadline

`LocalSoftDeadline` stops the pipeline after a given duration has elapsed since the last successful evolution
transition. The clock resets on every `Status.Success` transition, so the deadline window starts fresh after
each successful step. On a `Complete` or error transition the clock also resets, but the deadline is checked
at the very next `apply` call.

If the elapsed time reaches `duration` when `apply` is called, the current yield's status is upgraded to
`Status.Complete`. If `duration ≤ 0`, the factory returns [`DeadEnd`](../std/DeadEnd.md) directly.

This contrasts with [`GlobalSoftDeadline`](../std/GlobalSoftDeadline.md), whose deadline is fixed at
construction and never resets.

The factory has two overloads:

- `LocalSoftDeadline(duration: FiniteDuration, stage)` — wraps a single stage;
- `LocalSoftDeadline(duration: FiniteDuration)` — returns a `Decoration` to apply to multiple stages.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
import h8io.stages.operators.*
```

```scala mdoc:invisible
object PassThrough extends SAMStage[String, String, Nothing] {
  override def apply(in: String): Yield[String, String, Nothing] =
    Yield.Some(in, Status.Success, this)
}
```

```scala mdoc
var t = 0L
val stage = LocalSoftDeadline[String, String, Nothing](() => t, () => t, 3L, PassThrough)

val r1 = stage("first")        // ts=0, now=0, elapsed=0 < 3 → Success
t = 4L
val r2 = r1.evolve()("second") // ts=0, now=4, elapsed=4 ≥ 3 → Complete
```
