package h8io.stages.projections

import h8io.stages.Yield

object IOr {
  type Ior[+L, +R] = cats.data.Ior[L, R]

  object Left extends LeftProjection[Ior] {
    def apply(in: Ior[Any, ?]): Yield[Ior[Any, ?], Any, Nothing] =
      in.fold(out => some(out), _ => none, (out, _) => some(out))
  }

  object Right extends RightProjection[Ior] {
    def apply(in: Ior[?, Any]): Yield[Ior[?, Any], Any, Nothing] =
      in.fold(_ => none, out => some(out), (_, out) => some(out))
  }
}
