package io.h8.stages

package object test {
  implicit class StageExtension[-I, +O](stage: Stage[I, O]) {
    def log(id: String)(implicit logger: Logger): Stage[I, O] = logger.LoggerStage(id, stage)
  }
}
