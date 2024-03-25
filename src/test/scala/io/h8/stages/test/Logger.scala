package io.h8.stages.test

import io.h8.stages.{Behavior, Stage, State}

import scala.collection.mutable

final class Logger {
  private val applyLog: mutable.Buffer[String] = mutable.Buffer.empty
  private val concludeLog: mutable.Buffer[String] = mutable.Buffer.empty
  private val onFailureLog: mutable.Buffer[String] = mutable.Buffer.empty

  private[test] case class LoggerStage[I, O](id: String, stage: Stage[I, O]) extends Stage[I, O] {
    override def apply(in: I): State[I, O] = {
      applyLog += id
      stage(in) match {
        case State.Yield(out, conclude, onFailure) =>
          State.Yield(
            out,
            overrideConclude(conclude),
            { failure =>
              onFailureLog += id
              onFailure(failure).map(toLoggerStage)
            })
        case State.Done(conclude) => State.Done(overrideConclude(conclude))
        case State.Failure(cause, conclude) => State.Failure(cause, overrideConclude(conclude))
      }
    }

    private def overrideConclude(conclude: () => Behavior[I, O]): () => Behavior[I, O] = { () =>
      concludeLog += id
      conclude().map(toLoggerStage)
    }

    private def toLoggerStage(stage: Stage[I, O]) = stage match {
      case stage: LoggerStage[I, O] => stage
      case stage => LoggerStage(id, stage)
    }
  }

  def getApplyLog: Seq[String] = applyLog.toSeq
  def getConcludeLog: Seq[String] = concludeLog.toSeq
  def getOnFailureLog: Seq[String] = onFailureLog.toSeq
}
