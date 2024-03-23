package io.h8.stages.test

import io.h8.stages.{Stage, State}
import org.scalatest.{Assertion, Assertions}

import scala.collection.mutable

final class Counter extends Assertions {
  private case class Impl[T](n: Int, expected: String, condition: Int => Boolean, var count: Int)
      extends Stage.Safe[T, T] {
    def apply(v: T): State[T, T] = {
      count += 1
      `yield`(v)
    }

    def validate(): Assertion = assert(condition(count), s"counter $n, value: $count, expected: $expected")
  }

  private val counters: mutable.Buffer[Impl[?]] = mutable.Buffer[Impl[_]]()

  private def create[T](expected: String, condition: Int => Boolean): Stage[T, T] = {
    val counter = Impl[T](counters.length, expected, condition, 0)
    counters.append(counter)
    counter
  }

  def repeat[T](n: Int): Stage[T, T] = create(s"$n times", _ === n)

  def never[T]: Stage[T, T] = create("never", _ === 0)

  def once[T]: Stage[T, T] = create("once", _ === 1)

  def validate(): Unit = counters foreach (_.validate())
}
