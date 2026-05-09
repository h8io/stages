# StatusInstances

`StatusInstances` provides `cats.Monoid` and `cats.Eq` instances for `Status[E]`.

The `Monoid` instance exposes the algebraic structure described in the core module: `Status.Success` is the
identity element, and `combine` is the binary operation. Import `StatusInstances.*` to bring the instances
into scope.

```scala mdoc
import h8io.stages.*
import h8io.stages.cats.StatusInstances.*
import _root_.cats.{Eq, Monoid}
```

```scala mdoc
val m = Monoid[Status[String]]
m.empty
m.combine(Status.Success, Status.error("oops"))
m.combine(Status.error("a"), Status.error("b"))

val eq = Eq[Status[String]]
eq.eqv(Status.Success, Status.Success)
eq.eqv(Status.Success, Status.error("x"))
```
