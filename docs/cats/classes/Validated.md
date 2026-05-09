# Validated

`Validated` provides projections for `cats.data.Validated`. `Valid(r)` holds a success value and `Invalid(l)`
holds an error value. The two projections route each side into the pipeline independently, yielding
`Status.Success` in both the matching and non-matching cases.

- `Validated.Valid[T]` extracts the success value; yields nothing for `Invalid`.
- `Validated.Invalid[T]` extracts the error value; yields nothing for `Valid`.

Both are polymorphic singletons. Use `Validated.Valid[T]` and `Validated.Invalid[T]` to obtain typed
`Projection[cats.data.Validated[?, T], T]` and `Projection[cats.data.Validated[T, ?], T]` respectively.

```scala mdoc
import h8io.stages.*
import h8io.stages.cats.*
```

```scala mdoc:invisible
import _root_.cats.data.{Validated => CatsV}
val success: CatsV[String, String] = CatsV.valid("hello")
val failure: CatsV[String, String] = CatsV.invalid("error")
```

```scala mdoc
val validStage   = Validated.Valid[String]
val invalidStage = Validated.Invalid[String]

validStage(success)
validStage(failure)

invalidStage(success)
invalidStage(failure)
```
