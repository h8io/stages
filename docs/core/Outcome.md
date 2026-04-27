# Outcome

`Outcome` is the terminal result returned by `Stage.execute()`.

Unlike a [`Yield`](Yield.md), an `Outcome` carries no [`Evolution`](Evolution.md). It is the value given to the
caller once the pipeline has finished processing a single input and all resources have been released. The
[`Status`](Status.md) inside indicates whether execution succeeded (`Status.Success`) or completed
(`Status.Complete`), with or without accumulated errors. If the `Evolution` disposal threw an exception, that is
recorded in `disposeFailure` without preventing the outcome from being returned.

## Outcome.Some and Outcome.None

There are two variants, mirroring the two variants of [`Yield`](Yield.md).

`Outcome.Some` is produced when the executed stage yielded a `Yield.Some`. It carries the output value:

```scala mdoc
import h8io.stages.*

object Double extends Stage[Int, Int, Nothing] {
  private def stub: Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def apply(status: Status[?]): Stage[Int, Int, Nothing] = ???
    override def dispose(): Unit = ()
  }

  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(in * 2, Status.Success, stub)

  override def skip(): Evolution[Int, Int, Nothing] = stub
}

val some = Double.execute(21)
```

`Outcome.None` is produced when the stage yielded a `Yield.None` — that is, when it deliberately produced no
output for the given input:

```scala mdoc
object Drop extends Stage[Int, String, String] {
  private def stub: Evolution[Int, String, String] = new Evolution[Int, String, String] {
    override def apply(status: Status[?]): Stage[Int, String, String] = ???
    override def dispose(): Unit = ()
  }

  override def apply(in: Int): Yield[Int, String, String] =
    Yield.None(Status.error(s"dropped $in"), stub)

  override def skip(): Evolution[Int, String, String] = stub
}

val none = Drop.execute(99)
```

## Disposal Failures

`disposeFailure` holds any non-fatal exception thrown during `Evolution.dispose()`.

When `execute` runs, it always attempts to call `dispose()` on the evolution returned by the stage, because the
continuation is not needed and resources must be released. If `dispose()` throws, the exception does not abort
the result — the outcome is still assembled from the output and status of the stage run. The exception is instead
stored in `disposeFailure` so the caller can inspect or rethrow it:

```scala mdoc
object LeakyDispose extends Stage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(
      in * 2,
      Status.Success,
      new Evolution[Int, Int, Nothing] {
        override def apply(status: Status[?]): Stage[Int, Int, Nothing] = ???
        override def dispose(): Unit = throw new RuntimeException("cleanup failed")
      })

  override def skip(): Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def apply(status: Status[?]): Stage[Int, Int, Nothing] = ???
    override def dispose(): Unit = ()
  }
}

val leaky = LeakyDispose.execute(5)
leaky.disposeFailure.map(_.getMessage)
```

The output value and status are still available even though `dispose()` failed:

```scala mdoc
leaky match {
  case Outcome.Some(out, status, _) => s"out=$out status=$status"
  case Outcome.None(status, _)      => s"no output status=$status"
}
```

When `dispose()` completes without error, `disposeFailure` is `None`:

```scala mdoc
Double.execute(21).disposeFailure
```
