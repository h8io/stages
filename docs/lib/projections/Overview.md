# Overview

The `h8io.stages.projections` package holds the stages that read one value out of a container. When the container
holds the side the projection is for, the value goes downstream; when it holds the other, the projection yields
nothing. Either way the status is [`Status.Success`](../../core/classes/Status.md): the absence of a value is not
an error, it is a branch the pipeline did not take.

| Projection | Reads |
|---|---|
| [`Tuple2.Left`](Tuple2.md) / [`Tuple2.Right`](Tuple2.md) | `_1` / `_2` of a pair — always present |
| [`Either.Left`](Either.md) / [`Either.Right`](Either.md) | one side of a `scala.util.Either` |
| [`Unlift`](Unlift.md) | the value inside an `Option` |

Pairing a projection with a binary operator is how a pipeline branches and joins: [`And`](../operators/And.md)
produces a tuple, and a `Tuple2` projection picks the half the next stage needs. [`Unlift`](Unlift.md) is the
inverse of [`Lift`](../operators/Lift.md).

The projections for the cats types — `cats.data.Ior` and `cats.data.Validated` — live in the
[`stages-cats`](../../cats/Overview.md) module.

All of them are singletons built on [`Projection`](../base/Projection.md), which supplies the `some`/`none` helpers.
The two-sided ones go through `LeftProjection`/`RightProjection`, which add the typed `apply[T]` view of the single
instance; `Unlift`, having one side only, defines that view itself.
