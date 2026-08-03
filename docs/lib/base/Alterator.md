# Alterator

`Alterator` is the base trait for stages that wrap a single inner stage — the *alterand*. It declares the
abstract `alterand` member. Resource disposal stays with the `Evolution`s returned by `apply` and `skip`: a
concrete alterator must make sure its evolution delegates `dispose` to the alterand's evolution.

The type parameter `S` is covariant and bounded to `Stage.Any`, preserving the concrete type of the alterand in the
wrapper's static type without information loss.

**Do not mix `Alterator` with traits that supply the stage's own continuation** — its `skip`, `evolve` or `dispose`,
as [`SAMStage`](SAMStage.md) and [`Stagnation`](Stagnation.md) do. An alterator's evolution has to be built from the
alterand's; a mixin that answers `this` instead — and both of those seal it that way — leaves the alterand neither
evolved nor disposed, and nothing signals it.

Traits that only shape `apply` are safe to mix in, because they leave the continuation to the alterator itself.
[`SafeStage`](SafeStage.md) and [`Fruitful`](Fruitful.md) are both used that way in this library, by
[`Safe`](../operators/Safe.md) and [`Lift`](../operators/Lift.md) respectively.

The `base` package provides four type aliases for wrapping stages. The first two name the wrapper itself and are
built on `Alterator`; the other two name the function that produces one:

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
