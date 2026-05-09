# Alterator

`Alterator` is the base trait for stages that wrap a single inner stage — the *alterand*. It declares the
abstract `alterand` member and seals `dispose` to forward to `alterand.dispose()`, so concrete subclasses never
need to handle disposal themselves.

The type parameter `S` is covariant and bounded to `Stage.Any`, preserving the concrete type of the alterand in the
wrapper's static type without information loss.

**Do not mix `Alterator` with traits that introduce independent state or their own evolution logic** — for example,
[`SAMStage`](SAMStage.md) or [`Stagnation`](Stagnation.md). `Alterator` assumes that `alterand` owns all resources and controls all evolution.
A co-mixed trait that adds its own resources or overrides evolution without coordinating with the alterand breaks
that contract silently: its resources will not be released and its transitions will be ignored.

The `base` package provides four type aliases built on `Alterator`:

| Alias | Meaning |
|---|---|
| `UnaryOperator[+S, -I, +O, +E]` | Wraps any `Stage[I, ?, ?]`; may change all three type parameters |
| `Decorator[-I, +O, +E]` | Wraps a `Stage[I, O, E]` and preserves its full type |
| `Alteration[-IS, +OS]` | A function `IS => OS` that transforms one stage type into another |
| `Decoration[I, O, E]` | An `Alteration[Stage[I, O, E], Stage[I, O, E]]` — the shape passed to `Evolution.map` |

`Decorator` is the alias to reach for when building middleware that wraps a stage without changing its interface:

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
class Logged[I, O, E](val alterand: Stage[I, O, E]) extends Decorator[I, O, E] {
  override def apply(in: I): Yield[I, O, E] = {
    println(s"apply($in)")
    alterand(in)
  }

  override def skip(): Evolution[I, O, E] = alterand.skip()
}

val logged = new Logged(DoubleInt)
logged(10)
logged.skip()
```

`Alteration` and `Decoration` are the shapes used when passing stage transformations as values — for example, to
`Evolution.map`.
