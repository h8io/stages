# Break

`Break` is a stage that signals pipeline completion while still passing the input through as output.
Every invocation yields `Status.Complete` with the input value intact — no value is dropped.

It is designed as the terminal element in a conditional pipeline: once a preceding stage decides that
processing should stop, `Break` is used to emit the final value and close the pipeline cleanly.
The [`CompleteIfSome`](../operators/CompleteIfSome.md) operator uses `Break` for exactly this purpose.

`Break` is a polymorphic singleton. Use `Break[T]` to obtain a typed `Fruitful.Endo[T, Nothing]`.

```scala mdoc
import h8io.stages.*
import h8io.stages.std.*
```

```scala mdoc
val stage = Break[String]
stage("hello")
stage("world")
```
