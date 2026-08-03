# Complete

`Complete` is a stage that emits `Status.Complete` while passing the input through as output unchanged.
Every invocation yields `Status.Complete` with the input value intact — no value is dropped.

It signals that the current unit of work is done. At the top level of a pipeline this becomes the run's terminal
status; inside an enclosing [`cycles`](../cycles/Overview.md) operator such as `Loop` or `Repeat` it ends the
current cycle.
The [`CompleteIfSome`](../operators/CompleteIfSome.md) operator uses `Complete` for exactly this purpose.

`Complete` is a polymorphic singleton. Use `Complete[T]` to obtain a typed `Fruitful.Endo[T, Nothing]`.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val stage = Complete[String]
stage("hello")
stage("world")
```
