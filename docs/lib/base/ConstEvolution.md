# ConstEvolution and StageOps

`ConstEvolution` is an `Evolution` that returns the same stage for every status. It is the result of calling
`.toEvolution` on any stage via the `StageOps` extension:

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

```scala mdoc:invisible
object DoubleInt extends Fn[Int, Int] {
  override protected def f(in: Int): Int = in * 2
}
```

```scala mdoc
val evo: Evolution[Int, Int, Nothing] = DoubleInt.toEvolution

evo.evolve(Status.Success)
evo.evolve(Status.complete)
```

Both calls return `DoubleInt`. Use `ConstEvolution` inside operators like [`Loop`](../operators/Loop.md) or [`Repeat`](../operators/Repeat.md) where a fixed stage
should always be the continuation, regardless of what happened in the previous run.

The overload `stage.toEvolution(dispose: () => Unit)` produces a `ConstEvolution` that calls the supplied function
on disposal rather than ignoring it — useful when the lifted stage holds resources that need to be released.
