package com.besok.server.flow.json.parser


case class Generator(f: GeneratorFunction, jv: Json) {}

trait GeneratorContext {
  def get(key: String): Json

  def put(key: String, value: Json): Option[Json]
}

sealed trait GeneratorFunction {
  def generate(ctx: GeneratorContext): Json
}

class SequenceF(var idx: Int) extends GeneratorFunction {
  override def generate(ctx: GeneratorContext): Json = {
    idx += 1
    IntValue(idx - 1)
  }
}