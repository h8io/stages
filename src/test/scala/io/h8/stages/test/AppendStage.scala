package io.h8.stages.test

import io.h8.stages.{Behavior, Stage, State}

final case class AppendStage(s: String) extends Stage.Safe[String, String] {
  def apply(in: String): State[String, String] =
    State.Yield(in + s, () => Behavior.Undefined(this), _ => Behavior.Complete)
}
