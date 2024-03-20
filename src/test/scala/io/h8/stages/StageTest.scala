package io.h8.stages

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageTest extends AnyFlatSpec with Matchers {
  private case class AppendStage(s: String) extends Stage[String, String] {
    override def apply(in: String): State[String, String] =
      State.Yield(in + s, () => Behavior.Undefined(this), _ => Behavior.Complete)
  }

  private case class AppendRedo(s: String) extends Stage[String, String] {
    override def apply(in: String): State[String, String] = State.Done(() => Behavior.Redo(AppendStage(s)))
  }

  private case object CompleteStage extends Stage[String, String] {
    override def apply(in: String): State[String, String] =
      State.Yield(in, () => Behavior.Complete, _ => Behavior.Complete)
  }

  private case class CompleteOnLengthStage(length: Int) extends Stage[String, String] {
    override def apply(in: String): State[String, String] =
      State.Yield(in, () => if (in.length < length) Behavior.Redo(this) else Behavior.Complete, _ => Behavior.Complete)
  }

  "Stage" should "produce the correct result" in {
    (AppendStage("a") ~> AppendStage("b") ~> AppendStage("c") ~> CompleteStage).execute("x") match {
      case State.Yield("xabc", _, _) => succeed
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "redo the execution correctly" in {
    (AppendStage("a") ~>
      (AppendRedo("1") ~> AppendStage("b")) ~>
      (AppendRedo("2") ~> AppendStage("c")) ~>
      AppendRedo("3") ~>
      CompleteStage).execute("x") match {
      case State.Yield("xa1b2c3", _, _) => succeed
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "loop the execution correctly" in {
    (AppendStage("a") ~> AppendStage("b") ~> AppendStage("c") ~> CompleteOnLengthStage(10)).recursion("x") match {
      case State.Yield("xabcabcabc", _, _) => succeed
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
