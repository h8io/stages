package h8io.xi.stages

import cats.Monoid

package object test {
  implicit def signalMonoid[E]: Monoid[Signal[E]] =
    new Monoid[Signal[E]] {
      def empty: Signal[E] = Signal.Success
      def combine(x: Signal[E], y: Signal[E]): Signal[E] = x ++ y
    }
}
