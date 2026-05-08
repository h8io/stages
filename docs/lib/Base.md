# Base

The `h8io.stages.base` package sits between the core abstractions and concrete stage implementations.
The core `Stage` interface — implement `apply` and `skip`, supply an `Evolution` with `evolve` and `dispose` — is
intentionally minimal. That minimalism is useful at the boundary, but it leaves every implementor to repeat the same
structural boilerplate: a stage that never changes needs the same `skip`-returns-`this` pattern; a stage that wraps
another needs to forward `dispose`; a stage backed by a pure function always writes the same
`Yield.Some(f(in), Status.Success, this)`.

The `base` package captures those patterns as composable, reusable traits. Concrete stages mix in what they need and
implement only the logic that is genuinely specific to them.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

## The Immutability Hierarchy

The most common class of stages is those that do not change between runs: a filter, a transformer, a pure mapping.
None of these need to carry state from one invocation to the next; their evolution always returns the same stage.
The `base` package captures this with a small hierarchy, each layer sealing more boilerplate.

### Stagnation

`Stagnation` is an `Evolution` mixin that seals `evolve` to unconditionally return `this`, regardless of the status,
and makes `dispose` a no-op. Mixing it into a stage expresses that the stage is always its own continuation.

`Stagnation` has a self-type of `Stage[I, O, E]`, so it can only be mixed into things that are already stages.

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

### SAMStage

`SAMStage` (Single Abstract Method Stage) adds a sealed `skip` on top of `Stagnation`: `skip` returns `this`,
making the stage its own evolution whether or not it was applied. The only method left open is `apply`.

```scala mdoc
object ParseInt extends SAMStage[String, Int, String] {
  override def apply(in: String): Yield[String, Int, String] =
    in.toIntOption match {
      case Some(n) => Yield.Some(n, Status.Success, this)
      case None    => Yield.None(Status.error(s"not a number: $in"), this)
    }
}

ParseInt("42")
ParseInt("hello")
```

`dispose` is a no-op by default. Override it when the stage holds an external resource that must be released.

### StaticStage and StaticYield

`StaticStage` seals `apply` as well as `skip`, and delegates processing to a single abstract method
`process(in: I): StaticYield[O, E]`. The sealed `apply` wraps the result into a full `Yield` with `this` as the
evolution — the implementor never has to write that part.

`StaticYield` mirrors `Yield` but omits the `Evolution` field, since `StaticStage` always supplies `this` as the
continuation. It has two variants: `StaticYield.Some(out, status)` when a value is produced, and
`StaticYield.None(status)` when the stage deliberately emits nothing.

```scala mdoc
object IsEven extends StaticStage[Int, String, Nothing] {
  override protected def process(in: Int): StaticYield[String, Nothing] =
    if (in % 2 == 0) StaticYield.Some(s"$in is even", Status.Success)
    else             StaticYield.None(Status.Success)
}

IsEven(4)
IsEven(7)
```

Use `StaticStage` when the stage may or may not produce a value depending on the input. When output is always
guaranteed, `Fn` (below) is more specific.

### Fn

`Fn` is the most constrained member of the hierarchy: a `StaticStage` that always produces `Yield.Some` with
`Status.Success`. Only the pure mapping function `f` is abstract. The sealed `apply` calls `f` and wraps the result
automatically — there is no way for a `Fn` stage to produce `Yield.None` or any non-`Success` status.

```scala mdoc
object DoubleInt extends Fn[Int, Int] {
  override protected def f(in: Int): Int = in * 2
}

DoubleInt(21)
DoubleInt.skip()
```

`Fn` also defines `Fn.Endo[T]` as a type alias for endomorphic stages that map a type to itself.

## Output Guarantees

### Fruitful

`Fruitful` expresses a weaker guarantee than `Fn`: the stage always returns `Yield.Some`, but it is free to choose
any status and any evolution. The return type of `apply` is narrowed to `Yield.Some[I, O, E]` so the compiler can
track this guarantee statically, making `Fruitful` stages safe to use wherever an output value is always expected.

```scala mdoc
object Loudify extends Fruitful[String, String, Nothing] with SAMStage[String, String, Nothing] {
  override def apply(in: String): Yield.Some[String, String, Nothing] =
    Yield.Some(in.toUpperCase + "!", Status.Success, this)
}

Loudify("hello")
```

`Fruitful` is useful for binary operators that need to guarantee both branches always produce output, or for stages
that always yield a value but may still signal errors or transition to a different stage.

## Exception Handling

### SafeStage

`SafeStage` separates normal execution from exception recovery. It seals `apply` and routes each invocation through
two abstract methods:

- `body(in: I)` — the happy path;
- `recover(in: I, e: Throwable)` — called when `body` throws a non-fatal exception.

Fatal exceptions — anything `scala.util.control.NonFatal` does not match — propagate normally without being caught.

```scala mdoc
object SafeParseInt extends SafeStage[String, Int, String] with SAMStage[String, Int, String] {
  override def body(in: String): Yield[String, Int, String] =
    Yield.Some(in.toInt, Status.Success, this)

  override def recover(in: String, e: Throwable): Yield[String, Int, String] =
    Yield.None(Status.error(s"parse failed: ${e.getMessage}"), this)
}

SafeParseInt("99")
SafeParseInt("not-a-number")
```

`SafeStage` composes well with `SAMStage`: mix both when the stage is stateless and only the error recovery behavior
differs from the normal path.

## Composition Patterns

### Alterator and the Unary Operator Family

`Alterator` is the base trait for stages that wrap a single inner stage — the *alterand*. It declares the
abstract `alterand` member and seals `dispose` to forward to `alterand.dispose()`, so concrete subclasses never
need to handle disposal themselves.

The type parameter `S` is covariant and bounded to `Stage.Any`, preserving the concrete type of the alterand in the
wrapper's static type without information loss.

**Do not mix `Alterator` with traits that introduce independent state or their own evolution logic** — for example,
`SAMStage` or `Stagnation`. `Alterator` assumes that `alterand` owns all resources and controls all evolution.
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

### BinaryOperator

`BinaryOperator` is the base trait for stages that run two sub-stages on the same input type `I`. It seals
`dispose` to release both sub-stages safely: `right.dispose()` is called first; if it throws, `left.dispose()` is
still attempted and any additional exception is attached as a suppressed exception to the first.

`BaseBinaryOperator[-I, +LO, +RO, +O, +E]` is a type alias for the common case where both sub-stages share the
same input and error type but may have different output types. `BaseBinaryOperator.Evolution` is a companion base
`Evolution` that pairs two sub-evolutions and delegates both `evolve` and `dispose` to them with the same
exception-safe ordering.

Concrete operators such as `And`, `Or`, and `IAnd` extend `BinaryOperator` and implement `apply` to decide how the
results of the two sides are combined.

## Extraction

### Projection

`Projection` is a `StaticStage` for extracting a value from a container type. When the expected value is present it
returns `Yield.Some`; when it is absent it returns `Yield.None` with `Status.Success`, preserving the overall
pipeline status without signalling an error.

Two convenience members simplify `process` implementations: `some(out)` produces `StaticYield.Some(out, Status.Success)`
and `none` is a pre-built `StaticYield.None(Status.Success)` — both with `this` as the implicit evolution.

```scala mdoc
object OptionGet extends Projection[Option[Int], Int] {
  override protected def process(in: Option[Int]): StaticYield[Int, Nothing] =
    in match {
      case Some(v) => some(v)
      case None    => none
    }
}

OptionGet(Some(7))
OptionGet(None)
```

For covariant binary type constructors `C[+_, +_]`, `LeftProjection[C]` and `RightProjection[C]` provide reusable
singleton projections. Each is defined over the widened type `C[Any, ?]` or `C[?, Any]` and exposed as a concrete
`Projection[C[T, ?], T]` via a typed `apply[T]` method. The cast is sound because `C` is covariant in both
positions, so a `C[Any, ?]` can be read safely as a `C[T, ?]` for any `T`.

## Lifting Stages into Evolutions

### ConstEvolution and StageOps

`ConstEvolution` is an `Evolution` that returns the same stage for every status. It is the result of calling
`.toEvolution` on any stage via the `StageOps` extension:

```scala mdoc
val evo: Evolution[Int, Int, Nothing] = DoubleInt.toEvolution

evo.evolve(Status.Success)
evo.evolve(Status.complete)
```

Both calls return `DoubleInt`. Use `ConstEvolution` inside operators like `Loop` or `Repeat` where a fixed stage
should always be the continuation, regardless of what happened in the previous run.

The overload `stage.toEvolution(dispose: () => Unit)` produces a `ConstEvolution` that calls the supplied function
on disposal rather than ignoring it — useful when the lifted stage holds resources that need to be released.
