# Status

`Status` is the execution state that a [`Stage`](Stage.md) attaches to every [`Yield`](Yield.md) it returns.
As stages are composed into a pipeline via `~>`, their individual statuses are merged into a single overall status
for the run.

Three things can be signalled:

- that the stage completed normally and the pipeline may continue;
- that the pipeline has finished and should not request further input;
- that one or more errors occurred.

```scala mdoc
import h8io.stages.*
```

## The Three Variants

`Status.Success` is the ordinary outcome. The stage did its work and the pipeline is free to move on.

`Status.Complete` signals that the pipeline has deliberately finished — it has produced everything it needs and does not
want more input. It is not an error: it is a stage's way of saying the work is done.

`Status.Error` means something went wrong. It holds at least one error value and can accumulate additional errors as
each stage in a composed pipeline reports its own failure independently.

## Combining Statuses

When a pipeline composes two stages with `~>` and both run on the same input, their statuses are merged by `combine`.
The rule is simple: the result is whichever of the two is more severe.

`Success` is the identity element. Combining it with any other status leaves that status unchanged:

```scala mdoc
Status.Success.combine(Status.Success)
Status.Success.combine(Status.Complete)
Status.Success.combine(Status.Error("something went wrong"))
```

`Complete` dominates `Success` but yields to `Error`:

```scala mdoc
Status.Complete.combine(Status.Success)
Status.Complete.combine(Status.Complete)
Status.Complete.combine(Status.Error("something went wrong"))
```

`Error` is the most severe status and dominates everything else. When two `Error` values are combined, their error lists
are concatenated, preserving the left-to-right order of the stages in the pipeline:

```scala mdoc
Status.Error("first").combine(Status.Success)
Status.Error("first").combine(Status.Complete)
Status.Error("first").combine(Status.Error("second"))
```

Together, `combine` and `Success` make `Status[E]` a monoid for any fixed `E`: `combine` is associative, and
`Success` is its identity element. The monoid is not commutative in general — when two `Error` values are combined,
the left-to-right order of their error lists is significant.

## Accumulating Errors

`Status.Error` extends `Iterable[E]`, so it can be iterated directly with `foreach` or a `for`-comprehension.
The primary error is available as `head`; any additional errors accumulated through composition are in `tail`.
`toList` returns them all in order:

```scala mdoc
val err = Status.Error("disk full", List("timeout", "connection reset"))
err.head
err.tail
err.toList
for (message <- err) println(message)
```

A single-argument factory is also provided for the common case of one error:

```scala mdoc
val single = Status.Error("not found")
single.head
single.tail
```

## Transforming Error Values

`map` applies a function to every error value contained in a `Status.Error`, leaving `Success` and `Complete`
unchanged. This is useful when a pipeline boundary needs to change the error representation:

```scala mdoc
val numeric = Status.Error("42", List("7", "100")).map(_.toInt)
numeric.toList
```

Because `Success` and `Complete` have no error values, `map` returns them as-is:

```scala mdoc
(Status.Success: Status[String]).map(identity)
(Status.Complete: Status[String]).map(identity)
```
