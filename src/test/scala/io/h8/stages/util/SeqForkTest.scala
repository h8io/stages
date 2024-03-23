package io.h8.stages.util

import io.h8.stages.State
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SeqForkTest extends AnyFlatSpec with Matchers {
  classOf[SeqFork[?, ?, ?]].getSimpleName should "produce correct results" in {
    (SeqFork(
      AppendStage("a") ~> AppendStage("b"),
      AppendStage("1") ~> AppendStage("2") ~> AppendStage("3")) ~> CompleteStage[(String, String)]).execute("x") match {
      case State.Yield(("xab", "x123"), _, _) => succeed
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
