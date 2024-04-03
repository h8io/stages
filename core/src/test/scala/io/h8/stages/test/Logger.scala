package io.h8.stages.test

import io.h8.stages.{Conclusion, Stage, State}

import scala.collection.mutable

final class Logger {
  private val applyLog: mutable.Buffer[String] = mutable.Buffer.empty
  private val concludeLog: mutable.Buffer[String] = mutable.Buffer.empty

  private[test] case class LoggerStage[-I, +O](id: String, stage: Stage[I, O]) extends Stage[I, O] {
    override def apply(in: I): State[I, O] = {
      applyLog += id
      stage(in) match {
        case State.Yield(out, conclude) =>
          State.Yield(
            out,
            { (previous: Option[Conclusion[?, ?]]) =>
              concludeLog += id
              conclude(previous).map(toLoggerStage)
            })
        case State.Done(conclusion) =>
          concludeLog += id
          State.Done(conclusion.map(toLoggerStage))
      }
    }

    private def toLoggerStage[II <: I, OO >: O](stage: Stage[II, OO]): LoggerStage[II, OO] = stage match {
      case stage: LoggerStage[II, OO] => stage
      case stage => LoggerStage(id, stage)
    }
  }

  def getApplyLog: Seq[String] = applyLog.toSeq
  def getConcludeLog: Seq[String] = concludeLog.toSeq
}
