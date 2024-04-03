package io.h8.stages.test

import io.h8.stages.{Conclusion, Stage, State}

final case class AppendStage(s: String) extends Stage[String, String] {
  def apply(in: String): State[String, String] = State.Yield(in + s, _ => Conclusion.Undefined(this))
}
