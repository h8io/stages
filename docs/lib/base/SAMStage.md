# SAMStage

`SAMStage` (Single Abstract Method Stage) adds a sealed `skip` on top of `Stagnation`: `skip` returns `this`,
making the stage its own evolution whether or not it was applied. The only method left open is `apply`.

```scala mdoc
import h8io.stages.*
import h8io.stages.base.*
```

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
