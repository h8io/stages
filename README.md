[![GitHub release](https://img.shields.io/github/v/release/h8io/stages)](https://github.com/h8io/stages/releases/latest)

# stages

`stages` is an experimental Scala library for building pipelines out of steps that can evolve as they run.

The core idea is simple: an ordinary function turns an input into an output, while a `Stage` does more. It not only
processes a value but also reports what happened and contributes to how the pipeline evolves next.

That is what makes `stages` different from ordinary function composition. Here, a pipeline is not just a chain of
transformations, but a living structure that can:

- continue;
- finish normally;
- react to errors;
- evolve in response to what happened.

## Why

In many real computations, producing the next value is not enough. You also need to express:

- that processing succeeded;
- that the work completed normally, without failure;
- that something went wrong;
- that the next step should now behave differently.

This is especially useful for stateful computations, iterative algorithms, streaming-like workflows, decorators, and
other scenarios where not only the result matters, but also the future of the computation.

## How to think about it

The easiest way to think about a `Stage` is as a computation step that can evolve in response to what happened.

After each call, it returns not just a result, but a richer answer:

- possibly a new value;
- an execution status;
- a description of how the pipeline should evolve next.

Because of that, `stages` is not only about transforming data, but also about composing behavior.

## What this gives you

This approach lets you model things explicitly that are often scattered across surrounding code in ordinary pipelines:

- normal completion without failure;
- continuing after partial success;
- repeating a step while it still makes sense;
- evolving after each call in response to what happened;
- building stateful pipelines in a direct way.

As a result, pipeline behavior becomes part of the model rather than an accidental side effect of external control flow.

## The basic intuition

In the shortest possible form:

- an ordinary function answers “what should I do with this value?”;
- a `Stage` answers “what happened now, and how should the pipeline evolve next?”.

That idea sits at the center of `stages`.

The `examples` module contains illustrative examples. They may be far from real-world usage, but they are meant to make
the core ideas easier to grasp.

## Project status

The project is still in the design stage, and its core abstractions are being refined.

This README is intentionally introductory and explanatory. Stricter definitions, formal semantics, and precise examples
can live in separate documentation.

## Who this may be for

`stages` may be interesting if you like:

- functional style;
- declarative pipelines;
- stateful computation;
- explicit semantics for continuation, completion, and failure.

If the idea of composable stateful pipelines in Scala appeals to you, `stages` may already be interesting at the level
of the model itself.

## API documentation
See [the documentation](https://h8io.github.io/stages/api/scala-2.13/).

## License

Apache-2.0. See `LICENSE`.
