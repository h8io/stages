# Fn

`Fn` is the most constrained member of the hierarchy: a `StaticStage` that always produces `Yield.Some` with
`Status.Success`. Only the pure mapping function `f` is abstract. The sealed `apply` calls `f` and wraps the result
automatically — there is no way for a `Fn` stage to produce `Yield.None` or any non-`Success` status.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

```scala mdoc
object DoubleInt extends Fn[Int, Int] {
  override protected def f(in: Int): Int = in * 2
}

DoubleInt(21)
DoubleInt.skip()
```

`Fn` also defines `Fn.Endo[T]` as a type alias for endomorphic stages that map a type to itself.
