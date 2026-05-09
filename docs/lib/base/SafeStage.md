# SafeStage

`SafeStage` separates normal execution from exception recovery. It seals `apply` and routes each invocation through
two abstract methods:

- `body(in: I)` — the happy path;
- `recover(in: I, e: Throwable)` — called when `body` throws a non-fatal exception.

Fatal exceptions — anything `scala.util.control.NonFatal` does not match — propagate normally without being caught.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

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

`SafeStage` composes well with [`SAMStage`](SAMStage.md): mix both when the stage is stateless and only the error recovery behavior
differs from the normal path.
