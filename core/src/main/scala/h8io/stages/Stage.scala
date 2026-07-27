package h8io.stages

/** A single processing unit of a pipeline: a function from an input value to a [[Yield]] — an optional output, a
  * [[Status]], and an [[Evolution]] that decides what the pipeline looks like for the next run.
  *
  * ==Lifecycle==
  *
  * Every stage instance takes exactly one of three paths per pipeline run:
  *
  *   - '''applied''' — [[apply]] receives an input value and returns a [[Yield]]. It may perform side effects and
  *     produce any output, status and evolution it likes.
  *   - '''skipped''' — [[skip]] returns the evolution the stage would have returned had it run, without consuming an
  *     input. Any stage that takes part in a run but does not process the current input takes this path.
  *   - '''failed''' — `apply` throws. No `Yield`, and therefore no `Evolution`, reaches the caller.
  *
  * Resource cleanup belongs to the [[Evolution]] returned by either of the first two: [[Evolution.dispose]] is the
  * exclusive release point, and the stage is permanently unusable afterwards. The failed path is the one case where
  * nobody outside can release anything — there is no evolution to dispose — so a stage that throws must release its own
  * resources before letting the exception escape. The core takes no other part in exception handling; the lib module
  * offers `h8io.stages.operators.Safe` as a crutch for the simplest, stateless cases. Fatal exceptions are outside this
  * contract.
  *
  * ==Composition==
  *
  * [[~>]] builds a pipeline out of two stages. When the upstream yields an output, it becomes the downstream's input
  * and the two yields are merged; when it yields nothing, the downstream is not applied for this input — it is skipped
  * and its evolution composed in, so it still evolves in step with the rest of the pipeline.
  *
  * @tparam I
  *   the input type (contravariant)
  * @tparam O
  *   the output type (covariant)
  * @tparam E
  *   the error type (covariant)
  */
trait Stage[-I, +O, +E] extends (I => Yield[I, O, E]) {

  /** Processes `in` and returns the [[Yield]] carrying the optional output, the status and the continuation.
    *
    * A stage that throws from here must have released its own resources first — see the ''Lifecycle'' section above.
    */
  def apply(in: I): Yield[I, O, E]

  /** Returns this stage's [[Evolution]] without consuming an input.
    *
    * Called instead of [[apply]] whenever the stage takes part in a run but has nothing to process: the upstream
    * yielded no output, or a binary operator excluded this branch. The evolution must be the one the stage would have
    * returned had it run. Like `apply`, `skip()` may perform side effects — a decorator, for instance, may advance or
    * release the inner stage it owns.
    */
  def skip(): Evolution[I, O, E]

  /** Composes this stage with `that`, feeding this stage's output into it. See the ''Composition'' section above for
    * what happens when there is no output to feed.
    */
  @inline final def ~>[_O, _E >: E](that: Stage[O, _O, _E]): Stage[I, _O, _E] = Stage.AndThen(this, that)

  /** Reverse composition, equivalent to `that ~> this`. Internal: it exists so [[Evolution.AndThen]] can compose its
    * continuations in the order its fields are named.
    */
  @inline private[stages] final def <~[_I, _E >: E](that: Stage[_I, I, _E]): Stage[_I, O, _E] = that ~> this
}

object Stage {

  /** A stage whose input and output types coincide — an endomorphism on `T`. */
  type Endo[T, +E] = Stage[T, T, E]

  /** Any stage, whatever its type parameters. Useful as a common supertype. */
  type Any = Stage[?, ?, ?]

  /** A stage that yields an output for every input — and so does every generation it evolves into.
    *
    * Both halves matter. Narrowing [[apply]] alone would state the guarantee for the current run only and lose it at
    * the next one, where the type is a plain [[Stage]] again; narrowing [[skip]] to an [[Evolution.Fruitful]] carries
    * it along the whole lineage. Without that closure `fruitful ~> fruitful` would stop being fruitful after the first
    * run, since the composition's continuation is built from the continuations of both sides.
    *
    * The guarantee covers the output only: the status is free, and so is the choice of continuation among fruitful
    * stages. `skip()` still consumes nothing and produces nothing — being fruitful says what an ''applied'' stage
    * yields, not that it must be applied.
    *
    * Implementing this by hand is rarely needed: `h8io.stages.base.FruitfulSAMStage` in the lib module covers the stage
    * that is its own evolution, and `h8io.stages.base.Fn` the pure-function case.
    */
  trait Fruitful[-I, +O, +E] extends Stage[I, O, E] {

    override def apply(in: I): Yield.Some.Fruitful[I, O, E]

    override def skip(): Evolution.Fruitful[I, O, E]

    /** Composes two fruitful stages into a fruitful one — the composition yields an output whenever both sides do.
      *
      * An overload rather than an override, so a mixed composition still resolves to the [[Stage.~>]] returning a plain
      * [[Stage]]: with one operand unclassified, the composition can yield nothing.
      */
    @inline final def ~>[_O, _E >: E](that: Stage.Fruitful[O, _O, _E]): Stage.Fruitful[I, _O, _E] =
      Stage.AndThen.Fruitful(this, that)
  }

  object Fruitful {

    /** A fruitful stage whose input and output types coincide. */
    type Endo[T, +E] = Stage.Fruitful[T, T, E]
  }

  /** The pipeline built by [[Stage.~>]]: `upstream` processes the input, `downstream` processes what it yielded.
    *
    * Internal to the library — the composition is used through `~>` and its result type is a plain [[Stage]], so the
    * representation stays free to change.
    */
  private[stages] final case class AndThen[-I, OI, +O, +E](upstream: Stage[I, OI, E], downstream: Stage[OI, O, E])
      extends Stage[I, O, E] {

    override def apply(in: I): Yield[I, O, E] =
      upstream(in) match {
        case some: Yield.Some[I, OI, E] => some.compose(downstream(some.out))
        case none: Yield.None[I, OI, E] => none.compose(downstream.skip())
      }

    override def skip(): Evolution[I, O, E] = upstream.skip().compose(downstream.skip())
  }

  private[stages] object AndThen {

    /** The pipeline built by [[Stage.Fruitful.~>]]. There is no `None` branch to handle: the upstream always yields an
      * output for the downstream to consume.
      */
    final case class Fruitful[-I, OI, +O, +E](upstream: Stage.Fruitful[I, OI, E], downstream: Stage.Fruitful[OI, O, E])
        extends Stage.Fruitful[I, O, E] {

      override def apply(in: I): Yield.Some.Fruitful[I, O, E] = {
        val yld = upstream(in)
        yld.compose(downstream(yld.out))
      }

      override def skip(): Evolution.Fruitful[I, O, E] = upstream.skip().compose(downstream.skip())
    }
  }
}
