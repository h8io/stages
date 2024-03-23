package io.h8.stages.test

import io.h8.stages.{Stage, State}
import org.scalatest.{Assertion, Assertions}

import scala.collection.mutable

final class Counter extends Assertions {
  private case class Impl[T](n: Int, expected: String, condition: Int => Boolean, var count: Int) extends Stage[T, T] {
    def apply(v: T): State[T, T] = {
      count += 1
      `yield`(v)
    }

    def validate(): Assertion = assert(condition(count), s"counter $n, value: $count, expected: $expected")
  }

  private val counters: mutable.Buffer[Impl[?]] = mutable.Buffer[Impl[_]]()

  def exactly[T](n: Int): Stage[T, T] = {
    val counter = Impl[T](counters.length, s"equal to $n", _ === n, 0)
    counters.append(counter)
    counter
  }

  def validate(): Unit = counters foreach (_.validate())
}
