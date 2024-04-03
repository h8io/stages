package io.h8.stages

import io.h8.stages.test.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageTest extends AnyFlatSpec with Matchers {
  classOf[Stage[?, ?]].getSimpleName should "produce the correct result" in {
    implicit val logger: Logger = new Logger
    val expectedApplyLog = List("a", "b", "c", "complete")
    (AppendStage("a").log("a") ~>
      AppendStage("b").log("b") ~>
      AppendStage("c").log("c") ~>
      CompleteStage[String].log("complete"))("x") match {
      case State.Yield("xabc", conclude) =>
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog
        logger.getConcludeLog shouldBe empty
        conclude(None) shouldBe Conclusion.Complete
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog
        logger.getConcludeLog should contain theSameElementsInOrderAs expectedApplyLog.reverse
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "redo the execution correctly" in {
    implicit val logger: Logger = new Logger
    val expectedApplyLog = List(
      List("a", "redo 1"),
      List("a", "1", "b", "redo 2"),
      List("a", "1", "b", "redo 2"),
      List("a", "1", "b", "2", "c", "redo 3"),
      List("a", "1", "b", "2", "c", "redo 3"),
      List("a", "1", "b", "2", "c", "redo 3"),
      List("a", "1", "b", "2", "c", "3", "complete")
    )
    (AppendStage("a").log("a") ~>
      (RedoStage(1, AppendStage("1").log("1")).log("redo 1") ~> AppendStage("b").log("b")) ~>
      (RedoStage(2, AppendStage("2").log("2")).log("redo 2") ~> AppendStage("c").log("c")) ~>
      RedoStage(3, AppendStage("3").log("3")).log("redo 3") ~>
      CompleteStage[String].log("complete")).execute("x") match {
      case Result.Yield("xa1b2c3", Conclusion.Complete) =>
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog.flatten
        logger.getConcludeLog should contain theSameElementsInOrderAs expectedApplyLog.flatMap(_.reverse)
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "done the execution correctly" in {
    implicit val logger: Logger = new Logger
    (AppendStage("a").log("a") ~> DoneStage.log("done") ~> AppendStage("b").log("b"))("x") match {
      case State.Done(conclusion) =>
        logger.getApplyLog should contain theSameElementsInOrderAs List("a", "done")
        logger.getConcludeLog should contain theSameElementsInOrderAs List("done", "a")
        conclusion shouldBe Conclusion.Complete
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
