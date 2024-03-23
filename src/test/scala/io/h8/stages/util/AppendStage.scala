package io.h8.stages.util

import io.h8.stages.{Behavior, Stage, State}

case class AppendStage(s: String) extends Stage[String, String] {
  def apply(in: String): State[String, String] =
    State.Yield(in + s, () => Behavior.Undefined(this), _ => Behavior.Complete)
}