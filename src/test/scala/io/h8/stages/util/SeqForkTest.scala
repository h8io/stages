package io.h8.stages.util

import io.h8.stages.State
import io.h8.stages.test.{AppendStage, CompleteStage, Logger, RedoStage, StageExtension}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SeqForkTest extends AnyFlatSpec with Matchers {
  classOf[SeqFork[?, ?, ?]].getSimpleName should "produce correct results" in {
    implicit val logger: Logger = new Logger
    val expectedApplyLog = List("a", "b", "1", "2", "3", "complete")
    (SeqFork(
      AppendStage("a").log("a") ~> AppendStage("b").log("b"),
      AppendStage("1").log("1") ~> AppendStage("2").log("2") ~> AppendStage("3").log("3")
    ) ~> CompleteStage[(String, String)].log("complete"))("x") match {
      case State.Yield(("xab", "x123"), conclude, _) =>
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog
        logger.getConcludeLog shouldBe empty
        logger.getOnFailureLog shouldBe empty
        conclude()
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog
        logger.getConcludeLog shouldBe expectedApplyLog.reverse
        logger.getOnFailureLog shouldBe empty
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "produce correct result with redo" in {
    implicit val logger: Logger = new Logger
    val expectedApplyLog = List(
      List("a", "redo b"),
      List("a", "redo b"),
      List("a", "b", "c", "1", "redo 2"),
      List("a", "b", "c", "1", "2", "redo 3"),
      List("a", "b", "c", "1", "2", "redo 3"),
      List("a", "b", "c", "1", "2", "3", "4", "complete")
    )
    (SeqFork(
      AppendStage("a").log("a") ~> RedoStage(2, AppendStage("b").log("b")).log("redo b") ~> AppendStage("c").log("c"),
      AppendStage("1").log("1") ~>
        RedoStage(1, AppendStage("2").log("2")).log("redo 2") ~>
        RedoStage(2, AppendStage("3").log("3")).log("redo 3") ~>
        AppendStage("4").log("4")
    ) ~> CompleteStage[(String, String)].log("complete")).execute("x") match {
      case State.Yield(("xabc", "x1234"), _, _) =>
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog.flatten
        logger.getConcludeLog should contain theSameElementsInOrderAs expectedApplyLog.flatMap(_.reverse)
        logger.getOnFailureLog shouldBe empty
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
