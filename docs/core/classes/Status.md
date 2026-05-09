# Status

`Status` is the execution state that a [`Stage`](Stage.md) attaches to every [`Yield`](Yield.md) it returns.
As stages are composed into a pipeline via `~>`, their individual statuses are merged into a single overall status
for the run.

Two things can be signalled:

- that the stage completed normally and the pipeline may continue;
- that the pipeline has finished — either cleanly or with one or more accumulated errors.

```scala mdoc
import h8io.stages.*
```

## The Two Variants

`Status.Success` is the ordinary outcome. The stage did its work and the pipeline is free to move on.

`Status.Complete` signals that the pipeline has finished. When its `errors` sequence is empty this represents clean
termination; when `errors` is non-empty one or more errors were accumulated during the run.

Two convenience values cover the common cases:

- `Status.complete` — a `Complete` with no errors, for signalling clean termination.
- `Status.error(head, tail*)` — a `Complete` with one or more error values.

## Combining Statuses

When a pipeline composes two stages with `~>` and both run on the same input, their statuses are merged by `combine`.
The rule is simple: the result is whichever of the two is more severe.

`Success` is the identity element. Combining it with any other status leaves that status unchanged:

```scala mdoc
Status.Success.combine(Status.Success)
Status.Success.combine(Status.complete)
Status.Success.combine(Status.error("something went wrong"))
```

`Complete` dominates `Success`. Two `Complete` values are merged by concatenating their error sequences,
preserving the left-to-right order of the stages in the pipeline:

```scala mdoc
Status.complete.combine(Status.Success)
Status.complete.combine(Status.complete)
Status.error("first").combine(Status.error("second"))
```

Together, `combine` and `Success` make `Status[E]` a monoid for any fixed `E`: `combine` is associative, and
`Success` is its identity element. The monoid is not commutative in general — when two `Complete` values with errors
are combined, the order of their error sequences is significant.

## Accessing Errors

`Status.Complete` extends `Iterable[E]`, so errors can be iterated directly with `foreach` or a `for`-comprehension.
`errors` holds the full sequence; `toList` returns them all in order:

```scala mdoc
val err = Status.error("disk full", "timeout", "connection reset")
err.errors
err.toList
for (message <- err) println(message)
```

`Status.complete` has no errors and is therefore empty:

```scala mdoc
Status.complete.isEmpty
Status.complete.toList
```

## Transforming Error Values

`map` applies a function to every error value contained in a `Status.Complete`, leaving `Status.Success` and
error-free `Complete` values unchanged. This is useful when a pipeline boundary needs to change the error
representation:

```scala mdoc
val numeric = Status.error("42", "7", "100").map(_.toInt)
numeric.toList
```

Because `Status.Success` and `Status.complete` carry no error values, `map` returns them unchanged:

```scala mdoc
(Status.Success: Status[String]).map(identity)
(Status.complete: Status[String]).map(identity)
```
