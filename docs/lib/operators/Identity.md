# Identity

`operators.Identity` is a polymorphic singleton `Decoration` that returns the decorated stage unchanged.
It is the identity element for decoration composition — useful as a no-op placeholder when a `Decoration`
is required by an API but no transformation is needed.

This is distinct from [`std.Identity`](../std/Identity.md), which is the identity `Stage` (pass-through for
values). `operators.Identity` is a `Stage → Stage` function; `std.Identity` is a `T → T` stage.

Use `Identity[I, O, E]` to obtain a typed `Decoration[I, O, E]`.

```scala mdoc
import h8io.stages.*
import h8io.stages.operators.Identity
```

```scala mdoc
import h8io.stages.base.*
```

```scala mdoc
object Double extends SAMStage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] =
    Yield.Some(in * 2, Status.Success, this)
}

val dec = Identity[Int, Int, Nothing]
val stage = dec(Double)
stage(21)
```
