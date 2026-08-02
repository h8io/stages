# Yield

`Yield` is the immediate result returned by a [`Stage`](Stage.md) when it processes an input value.  
Every `Yield` bundles three things together:

- an optional output value of type `O`;
- a [`Status`](Status.md) describing how the stage completed;
- an [`Evolution`](Evolution.md) that knows which stage to use when the pipeline is ready to process the next input.

`Yield` is not the final answer the pipeline gives to the outside world — that is [`Outcome`](../../lib/base/Outcome.md) in the lib module.
`Yield` is the internal value that flows from one stage to the next, carrying the continuation forward.

```scala mdoc
import h8io.stages.*
```

## Carrying a Value vs Carrying Nothing

There are exactly two variants.

`Yield.Some` is produced when the stage has a result to hand downstream. It carries the output value together with the
status and the evolution:

```scala mdoc
val some = Yield.Some(42, Status.Success, new Evolution[Int, Int, Nothing] {
  override def evolve(status: Status[?]): Stage[Int, Int, Nothing] = ???
  override def dispose(): Unit = ()
})
some.out
some.status
```

`Yield.None` is produced when the stage deliberately emits nothing — for example, a filter that drops a particular
input. There is no output value, but the status and the evolution are still present so the pipeline can continue
correctly:

```scala mdoc
val none = Yield.None[Int, String, String](Status.error("filtered out"), new Evolution[Int, String, String] {
  override def evolve(status: Status[?]): Stage[Int, String, String] = ???
  override def dispose(): Unit = ()
})
none.status
```

When a `Yield.None` reaches a composed stage, the downstream stage is not applied to the current input.  
Instead it is wired into the evolution so that the full composed stage will be called when a value eventually
arrives.

## Accessing the Output

`outOption` returns the output wrapped in `Some`, or `None` when no value was produced:

```scala mdoc
some.outOption   // Some(42)
none.outOption   // None
```

Use `outOption` when you only need to know whether a value was produced. When you also need the `status` or
`evolution`, pattern-match on the concrete subtype directly instead.

## Constructing from an Option

The companion object provides an `apply` that selects the variant from an optional output: a `Some` becomes
`Yield.Some`, a `None` becomes `Yield.None`. It is convenient when the presence of an output is only known at
runtime — for example, when it comes from a lookup:

```scala mdoc
def lookup(key: Int): Option[String] = if (key > 0) Some(s"value-$key") else None

val lookupEvolution = new Evolution[Int, String, Nothing] {
  override def evolve(status: Status[?]): Stage[Int, String, Nothing] = ???
  override def dispose(): Unit = ()
}

Yield(lookup(1), Status.Success, lookupEvolution)
Yield(lookup(-1), Status.Success, lookupEvolution)
```

## Matching Without Naming the Variant

The mirror `unapply` makes `Yield` itself usable as an extractor: a single case binds the optional output, the
status and the evolution, whichever variant the value is. It is the inverse of `apply`; because it returns
`scala.Some`, the compiler knows the extractor always matches, and a single `case Yield(...)` is exhaustive:

```scala mdoc
def render(yld: Yield[Int, String, Nothing]): String = yld match {
  case Yield(out, status, _) => s"out: $out, status: $status"
}

render(Yield(lookup(1), Status.Success, lookupEvolution))
render(Yield(lookup(-1), Status.Success, lookupEvolution))
```

Prefer matching on `Yield.Some` / `Yield.None` when the two variants are handled differently — the generic
extractor is for the cases where they are treated uniformly.

## Evolving the Pipeline

Once the pipeline has processed an input and is ready for the next one, it calls `evolve()` on the `Yield` returned
by the last stage.  
`evolve()` calls `evolution.evolve(status)` — the `Evolution` receives the current status and returns the next stage.

```scala mdoc
object DoubleStage extends Stage[Int, Int, Nothing] {
  private def stub: Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def evolve(status: Status[?]): Stage[Int, Int, Nothing] = ???
    override def dispose(): Unit = ()
  }

  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(in * 2, Status.Success, stub)

  override def skip(): Evolution[Int, Int, Nothing] = stub
}

object ErrorRecovery extends Stage[Int, Int, Nothing] {
  private def stub: Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def evolve(status: Status[?]): Stage[Int, Int, Nothing] = ???
    override def dispose(): Unit = ()
  }

  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(0, Status.complete, stub)

  override def skip(): Evolution[Int, Int, Nothing] = stub
}

val yldSuccess = Yield.Some(
  21,
  Status.Success,
  new Evolution[Int, Int, Nothing] {
    override def evolve(status: Status[?]): Stage[Int, Int, Nothing] = status match {
      case Status.Success => DoubleStage
      case _              => ErrorRecovery
    }
    override def dispose(): Unit = ()
  })

val nextStage = yldSuccess.evolve()
```

Because the status is `Success`, the evolution returns `DoubleStage`.  
Applying it to a new input produces the expected result:

```scala mdoc
nextStage(21)
```

## Transforming a Yield

`map` transforms all three components of a `Yield` in one step. Each component gets its own mapping function.
This is mainly used inside pipeline combinators rather than in application code, but it is part of the public API.

Imagine a stage that reads a temperature sensor and yields the value in Celsius. A downstream combinator
needs Fahrenheit: it converts the value, marks each successful reading as complete, and wraps the evolution
with `Evolution.map` so that future stages also go through the same conversion:

```scala mdoc
object ThermStage extends Stage[Int, Int, Nothing] {
  private val evo: Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def evolve(status: Status[?]): Stage[Int, Int, Nothing] = ThermStage
    override def dispose(): Unit = ()
  }
  override def apply(in: Int): Yield[Int, Int, Nothing] = Yield.Some(25, Status.Success, evo)
  override def skip(): Evolution[Int, Int, Nothing] = evo
}

object ToFahrenheit extends Stage[Int, Int, Nothing] {
  private val evo: Evolution[Int, Int, Nothing] = new Evolution[Int, Int, Nothing] {
    override def evolve(status: Status[?]): Stage[Int, Int, Nothing] = ToFahrenheit
    override def dispose(): Unit = ()
  }
  override def apply(in: Int): Yield[Int, Int, Nothing] = Yield.Some(in * 9 / 5 + 32, Status.Success, evo)
  override def skip(): Evolution[Int, Int, Nothing] = evo
}

val celsius = ThermStage(0)

val fahrenheit = celsius.map(
  mapOut    = c => c * 9 / 5 + 32,
  mapStatus = _ => Status.complete,
  mapEvolution = evo => evo.map(_ ~> ToFahrenheit))

fahrenheit.outOption
fahrenheit.status
```

For `Yield.None`, `map` applies `mapStatus` and `mapEvolution` as usual — `mapOut` is accepted for
type-consistency but is never invoked because there is no value to transform. Here the sensor went
offline before any reading was taken:

```scala mdoc
val noReading = Yield.None[Int, Int, String](Status.error("sensor offline"), ThermStage.skip())
  .map(
    mapOut    = c => c * 9 / 5 + 32,   // not called — no value
    mapStatus = _ => Status.error("no data"),
    mapEvolution = evo => evo.map(_ ~> ToFahrenheit))

noReading.outOption
noReading.status
```
