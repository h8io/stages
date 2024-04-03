package io.h8.stages

import io.h8.stages.test.{AppendStage, Logger, RedoWhileStage, StageExtension}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SymmetricStageTest extends AnyFlatSpec with Matchers {
  classOf[SymmetricStage[?]].getSimpleName should "loop the execution correctly" in {
    implicit val logger: Logger = new Logger
    val expectedApplyLog = (1 to 3) map (_ => List("a", "b", "c", "redo"))
    (AppendStage("a").log("a") ~>
      AppendStage("b").log("b") ~>
      AppendStage("c").log("c") ~>
      RedoWhileStage[String](_.length < 10).log("redo")).loop("x") match {
      case Result.Yield("xabcabcabc", _) =>
        logger.getApplyLog should contain theSameElementsInOrderAs expectedApplyLog.flatten
        logger.getConcludeLog should contain theSameElementsInOrderAs expectedApplyLog.flatMap(_.reverse)
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
