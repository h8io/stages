# GlobalSoftDeadline

`GlobalSoftDeadline` is an endomorphic stage that passes values through as long as a wall-clock deadline has
not elapsed. The deadline is measured from the moment the instance is created: the current time is captured
once at construction and never updated. On each invocation, if the elapsed time is less than `duration`, the
stage yields `Status.Success`; otherwise it yields `Status.Complete`.

The deadline is *global* in the sense that it is fixed at construction. This contrasts with
`LocalSoftDeadline`, which resets its clock after each evolution transition.

The factory has three overloads:

- `GlobalSoftDeadline(now: () => Long, duration: Long)` — raw nanosecond clock and budget;
- `GlobalSoftDeadline(duration: FiniteDuration)` — uses `System.nanoTime`;
- `GlobalSoftDeadline(duration: java.time.Duration)` — uses `System.nanoTime`.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
var t = 0L
val deadline = GlobalSoftDeadline[String](() => t, 3L)

deadline("first")
t = 4L
deadline("second")
```

The first call sees elapsed time `0 < 3` and yields `Status.Success`; after advancing the clock past the
budget, the second call sees elapsed time `4 ≥ 3` and yields `Status.Complete`.
