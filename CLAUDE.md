# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`stages` is an experimental Scala library for building pipelines out of steps ("stages") that can evolve as they run.
Scala 2.13 (cross-built to 2.12) with `-Xsource:3` and `-Xfatal-warnings`. sbt 1.x on `main`; an sbt 2.0.0 migration
lives on the `update/sbt-2.0.0` branch, blocked because `sbt-typelevel-site` (Laika) has no sbt 2 support. That block
is why `.github/workflows/release.yaml` is pinned to `h8io/gha@v3` while the other two workflows are on `@v5`: from
v4 the release workflow runs `sbt cleanFull`, an sbt 2.0-only command.

## Commands

```bash
sbt test                          # run all tests (all modules)
sbt lib/test                      # run tests for one module (core / lib / cats / examples)
sbt "lib/testOnly h8io.stages.operators.LoopTest"          # single test suite
sbt "lib/testOnly *LoopTest -- -z \"substring of test name\""  # single test case
sbt +test                         # tests across both Scala versions (2.13 and 2.12)
sbt scalafmtAll scalafmtSbt       # format code and build files
sbt scalafmtCheckAll scalafmtSbtCheck  # check formatting (CI-style)
./test.sh                         # full CI check: format check, cross-build, coverage gate, docs, unidoc, site
./pages.sh                        # build the documentation site into target/pages
```

## Modules

- **core** — the minimal model: `Stage`, `Yield`, `Status`, `Evolution` (package `h8io.stages`). No dependencies.
- **lib** — the standard library on top of core: `base` (building blocks: `SAMStage`, `Fn`, `Projection`,
  `ConstEvolution`, `Alterator`/`BinaryOperator`, the `execute` terminal driver with its `Outcome` result type, and
  type aliases like `Decorator`/`Alteration`), `operators` (combinators wrapping stages: `And`, `Or`, `Lift`, `Safe`,
  `CompleteIfNone`, deadlines…), `cycles` (operators that drive a whole cycle of the inner stage per outer run:
  `Loop`, `Repeat`), `std` (leaf stages: `Const`, `Identity`, `Countdown`, `Coalesce`…), `projections`.
- **cats** — cats-core integration (`Validated`, `IOr`, `Monoid`/typeclass instances for `Status`).
- **examples** — runnable examples used by docs and tests; not published.
- **pages** — Typelevel-site documentation project (sources in `docs/`); not aggregated in root, built via
  `pages/tlSite`.

Dependency chain: `cats`/`examples` → `lib` → `core`.

## Core model (read before touching core)

A `Stage[-I, +O, +E]` maps an input to a `Yield[I, O, E]`, which carries three things: an optional output (`Yield.Some`/
`Yield.None`), a `Status[E]`, and an `Evolution[I, O, E]`.

- **Status** is a semigroup: `Success` (identity, keep going) or `Complete(errors)` (a unit of work finished; errors
  accumulate on combine). `Complete` dominates `Success`.
- **Evolution** decides which stage instance handles the *next* input, chosen by the previous status (
  `evolution.evolve(status)`) — this is how stages "evolve" between runs. It is also the exclusive owner of resource
  cleanup via `dispose()`. `evolve` and `dispose()` are not mutually exclusive: operators that own their inner stage
  (`Loop`, `Repeat`) evolve eagerly and keep the inner `dispose` as the terminal handle of the generation just
  constructed, so `dispose()` must stay valid after `evolve` and release everything still alive.
- **Lifecycle contract**: for each pipeline run, exactly one of `apply(in)` or `skip()` is called on every stage.
  `skip()` must return the stage's evolution without consuming input (used when an upstream produced no output or a
  binary operator excluded the branch); like `apply`, it may perform side effects. After `dispose()` the stage is
  permanently unusable. A stage that throws from `apply` must release its own resources before propagating — core
  takes no other part in exception handling (`execute` capturing dispose failures and the varargs `Evolution.dispose`
  helper — the shared disposal discipline with suppression — are the only exception-aware spots).
- Composition is `a ~> b`. When the upstream yields `None`, the downstream is not applied but its `skip()` evolution
  is still composed in. The nodes this builds — `Stage.AndThen`, `Evolution.AndThen`, `Evolution.Mapped` — are
  `private[stages]`: `~>`, `compose` and `map` all return the plain trait, so the representation is not API and stays
  free to change. Core's own tests construct them directly, which works because they are in package `h8io.stages`.
  Note `Evolution.AndThen` deliberately names its fields in the reverse of `Stage.AndThen` — see its scaladoc before
  modifying.
- `stage.execute(in)` is the terminal operation (an extension method from `h8io.stages.base`, not part of core): it
  runs once, disposes the evolution, and returns an `Outcome` (dispose failures are captured in
  `Outcome.disposeFailure`, not thrown).

## Testing setup

- ScalaTest + ScalaMock + ScalaCheck; cats-laws/discipline in the cats module.
- The `sbt-testkit` plugin adds a `TestKit` configuration: shared test utilities live in `core/src/testkit/scala` (
  `StagesCoreArbitraries`, `StagesCoreTestUtil`) and are consumed by other modules via `core % "test->testkit"`.

## Style

- Use Scala 3-style wildcard imports (`import foo.*`, `import Dependencies.*`), not `foo._` — the build enforces
  `-Xsource:3` and scalafmt uses the `scala213source3` dialect.
- Warnings are fatal (compiler and scalafmt), including unused-symbol warnings; run `sbt scalafmtAll` before committing.
- Scaladoc is extensive and normative in core — the comments document behavioral contracts (lifecycle, disposal order,
  variance), so keep them in sync with code changes.
- Prefer backticks over `[[...]]` for anything an implementor might overload, and always for `private[stages]` targets,
  which scaladoc cannot resolve. A link that is unambiguous per module can still fail under `+pages/unidoc`, where the
  comment is inherited into a subclass with its own overloads — `[[apply]]` on `Stage.skip` broke the site build that
  way, via `object Swap`.
