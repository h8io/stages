# LocalSoftDeadline

`LocalSoftDeadline` stops the pipeline when a time budget measured from the beginning of the current unit of work
is exhausted. The timestamp is captured at the `apply` call that starts the unit of work and is preserved for as
long as the evolution is invoked with [`Status.Success`](../../core/classes/Status.md). When the evolution is
invoked with any other status, the clock resets — the new window starts at the next `apply` call. The same reset
happens after the deadline has been exceeded.

The deadline is *soft*: it is checked after the inner stage has already been applied. If the elapsed time reaches
`duration`, a `Success` status is upgraded to an error-free [`Status.Complete`](../../core/classes/Status.md); any
other status is left unchanged. If `duration ≤ 0`, the factory returns [`DeadEnd`](../std/DeadEnd.md) directly.

This contrasts with [`GlobalSoftDeadline`](../std/GlobalSoftDeadline.md), whose deadline is fixed at
construction and never resets.

The factory has four overloads — both duration types in both shapes:

- `LocalSoftDeadline(duration: FiniteDuration, stage)` and `LocalSoftDeadline(duration: java.time.Duration, stage)` —
  wrap a single stage;
- `LocalSoftDeadline(duration: FiniteDuration)` and `LocalSoftDeadline(duration: java.time.Duration)` — return a
  `Decoration` to apply to multiple stages.

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
