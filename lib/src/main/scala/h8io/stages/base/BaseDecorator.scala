package h8io.stages.base

import h8io.stages.Decorator

trait BaseDecorator[I, O, E] extends BaseAlterator[I, O, E, I, O, E] with Decorator[I, O, E]
