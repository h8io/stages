# StaticStage and StaticYield

`StaticStage` seals `apply` as well as `skip`, and delegates processing to a single abstract method
`process(in: I): StaticYield[O, E]`. The sealed `apply` wraps the result into a full `Yield` with `this` as the
evolution — the implementor never has to write that part.

`StaticYield` mirrors `Yield` but omits the `Evolution` field, since `StaticStage` always supplies `this` as the
continuation. It has two variants: `StaticYield.Some(out, status)` when a value is produced, and
`StaticYield.None(status)` when the stage deliberately emits nothing.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

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
guaranteed, [`Fn`](Fn.md) is more specific.
