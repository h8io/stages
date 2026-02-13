[![GitHub release](https://img.shields.io/github/v/release/h8io/stages)](https://github.com/h8io/stages/releases/latest)

# Stages DSL

Stages is a small functional DSL for building composable processing pipelines.
Each processing step is a `Stage` that produces a `Yield` with a `Signal` and a continuation
(`OnDone`) that describes how the pipeline should evolve on success, completion, or error.
The evolution model keeps the pipeline explicit and restartable, and makes it easy to
decorate with behaviors like loops, retries, deadlines, and branching.

## Maven Central

Add to `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "io.h8" %% "stages-core" % "0.0.4",
  "io.h8" %% "stages-lib" % "0.0.4",
  "io.h8" %% "stages-cats" % "0.0.4"
)
```

## Core concepts

- `Stage[I, O, E]`: a step from input `I` to output `O` that can produce errors of type `E`.
  It is also a function `I => Yield[I, O, E]`. A stage may be stateless, but can also hold
  resources (files, sockets) and evolve into a new stage via `OnDone`.
- `Yield`: the result of a step. `Yield.Some(out, signal, onDone)` returns output, while
  `Yield.None(signal, onDone)` does not. Both carry a `Signal` and the next `OnDone`.
- `Signal`: a control signal for the pipeline.
  - `Success` means continue normally.
  - `Complete` stops the pipeline (a soft break).
  - `Error` accumulates one or more errors and switches to the error branch.
- `OnDone`: describes the next `Stage` to run after success, completion, or error. It is the
  result of a stage's evolution and is also responsible for resource release and transitions.
- `Alteration` / `Decoration`: functions that wrap or transform stages (e.g., loop, repeat,
  deadlines, lifting to `Option`, etc.). They compose via `~>` and apply via `⋅` or `<|`.

## Glossary

- `Yield`: a step-level result that still carries the continuation (`OnDone`).
- `Outcome`: a finalized result with a `Signal` and `dispose`, without a continuation.
- `Signal.Success`: normal continuation signal.
- `Signal.Complete`: a soft stop (break) for the pipeline.
- `Signal.Error`: an error signal that can accumulate multiple errors.
- `OnDone`: the evolution/continuation of a stage and the holder of cleanup logic.
- `Alteration`: a transformation `Stage => Stage`.
- `Decoration`: an `Alteration` that preserves input/output types.
- `Alterator`: a `Stage` wrapper that holds the altered stage (`alterand`) and delegates disposal.

## Modules

- `core`: the minimal model and operators:
  `Stage`, `Yield`, `Signal`, `OnDone`, `Outcome`, and alteration composition.
  Files: `core/src/main/scala/h8io/stages/...`.
- `lib`: standard library with concrete stages, decorations, projections, and binary ops.
  Files: `lib/src/main/scala/h8io/stages/...`.
- `cats`: integrations for Cats types such as `Ior` and `Validated`.
  Files: `cats/src/main/scala/h8io/stages/cats/...`.
- `examples`: small, runnable examples demonstrating real pipelines.
  Files: `examples/src/main/scala/h8io/stages/examples/...`.

## Examples

The `examples` module contains more complete pipelines:

- Factorials with looping and countdowns:
  `examples/src/main/scala/h8io/stages/examples/Factorial1.scala`.
- Series computation (Leibniz / Wallis):
  `examples/src/main/scala/h8io/stages/examples/Leibniz.scala`,
  `examples/src/main/scala/h8io/stages/examples/Wallis.scala`.
- A small caching example:
  `examples/src/main/scala/h8io/stages/examples/Cache.scala`.

## Alterations and binary ops

The standard library includes:

- Looping and repetition: `Loop`, `Repeat`.
- Deadlines: `GlobalSoftDeadline`, `LocalSoftDeadline`.
- Control flow: `Break`, `BreakIfNone`, `BreakIfSome`, `DeadEnd`.
- Output shaping: `Lift`, `KeepLastOutput`, `Unlift`.
- Binary combinators: `And`, `IAnd`, `Or` and projections for tuples and either types.

## License

Apache-2.0. See `LICENSE`.
