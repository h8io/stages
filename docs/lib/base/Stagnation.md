# Stagnation

`Stagnation` is an `Evolution` mixin that seals `evolve` to unconditionally return `this`, regardless of the status,
and makes `dispose` a no-op. Mixing it into a stage expresses that the stage is always its own continuation.

`Stagnation` has a self-type of `Stage[I, O, E]`, so it can only be mixed into things that are already stages.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

```scala mdoc
object Counter extends Stage[Unit, Int, Nothing] with Stagnation[Unit, Int, Nothing] {
  private var n = 0

  override def apply(in: Unit): Yield[Unit, Int, Nothing] = {
    n += 1
    Yield.Some(n, Status.Success, this)
  }

  override def skip(): Evolution[Unit, Int, Nothing] = this
}

Counter(())
Counter(())
Counter.skip()
```

`Stagnation` seals only `evolve` and `dispose`. The `skip` method stays open, so the stage has full control over
what evolution it exposes when bypassed. In practice, most stateless stages use `this` for `skip` as well — which
is exactly what the next layer provides.
