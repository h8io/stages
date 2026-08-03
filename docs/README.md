# Stages

`stages` is a Scala library for building pipelines out of composable, reusable steps.

To install the package:

```scala
libraryDependencies += "io.h8" %% "stages" % "@VERSION@"
```

## Background

The library grew out of experience with Apache Spark. Spark applications tend to start clean and grow messy:
logic spreads across functions, configuration leaks into transformations, and reuse becomes difficult.

What was missing was a way to express a pipeline as a readable, declarative structure — something a domain expert
could follow and adjust without needing to understand the underlying Scala.

`stages` is an attempt to provide exactly that.

## What it looks like

A pipeline built with `stages` can look like this:

```scala
InitSpark ~>
  From("users") ~>
  Where("created_at > current_timestamp() - INTERVAL 1 DAY") ~>
  Select("id", "name") ~>
  Save("users", format = "csv")
```

This is not pseudocode. It is real Scala, and it is meant to read as naturally as a description of what the pipeline
does.

## How it works

The core abstraction is a `Stage`: a computation step that does not just transform a value, but also reports what
happened and describes how the pipeline should evolve next.

This is what makes composition possible: each step can continue, complete, or signal that the next step should
behave differently — without any of that logic leaking into the surrounding code.

## Division of labor

`stages` is designed with two audiences in mind.

**Developers** implement the building blocks: individual `Stage` components with full access to the language and
libraries. They define what each step does, how it handles errors, and how it passes state forward.

**Domain experts** compose pipelines from those components. The DSL is designed to be readable and writable even
for people who are not fluent in Scala — the structure speaks for itself.

This separation keeps pipelines clean and makes them easier to review, adjust, and hand off.

## Where it fits

`stages` is designed for applications where the orchestration overhead is negligible compared to the work being
done — frameworks like Spark or Flink are a natural fit. For lightweight, performance-sensitive tasks, the
Stage lifecycle may introduce unnecessary object allocation, and a simpler approach is likely more appropriate.

The priority is developer ergonomics: ease of composition, clarity of intent, and readability of the result.

## Project status

The core abstractions are still being refined. The `examples` module contains illustrative examples meant to
make the ideas easier to grasp.
