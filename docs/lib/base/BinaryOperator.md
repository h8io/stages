# BinaryOperator

`BinaryOperator` is the base trait for stages that run two sub-stages on the same input type `I`. It seals
`dispose` to release both sub-stages safely: `right.dispose()` is called first; if it throws, `left.dispose()` is
still attempted and any additional exception is attached as a suppressed exception to the first.

`BaseBinaryOperator[-I, +LO, +RO, +O, +E]` is a type alias for the common case where both sub-stages share the
same input and error type but may have different output types. `BaseBinaryOperator.Evolution` is a companion base
`Evolution` that pairs two sub-evolutions and delegates both `evolve` and `dispose` to them with the same
exception-safe ordering.

Concrete operators such as [`And`](../operators/And.md), [`Or`](../operators/Or.md), and [`IAnd`](../operators/IAnd.md)
extend `BinaryOperator` and implement `apply` to decide how the results of the two sides are combined.
