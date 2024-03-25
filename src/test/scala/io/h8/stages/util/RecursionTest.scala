package io.h8.stages.util

import io.h8.stages.State
import io.h8.stages.test.{AppendStage, Logger, RedoWhileStage, StageExtension}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RecursionTest extends AnyFlatSpec with Matchers {
  classOf[Recursion[?]].getSimpleName should "loop the execution correctly" in {
    implicit val logger: Logger = new Logger
    val expectedApplyLog = (1 to 3) map (_ => List("a", "b", "c", "redo"))
    Recursion(
      AppendStage("a").log("a") ~>
        AppendStage("b").log("b") ~>
        AppendStage("c").log("c") ~>
        RedoWhileStage[String](_.length < 10).log("redo"))
      .execute("x") match {
      case State.Yield("xabcabcabc", _, _) =>
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog.flatten
        logger.getConcludeLog should contain theSameElementsInOrderAs expectedApplyLog.flatMap(_.reverse)
        logger.getOnFailureLog shouldBe empty
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
