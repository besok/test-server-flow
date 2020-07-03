package com.besok.server.flow.json.parser


sealed trait GeneratorFunction {
  def generate(ctx: GeneratorContext): JsonValue
}

class GeneratorContext

class Generator(f: GeneratorFunction, jv: JsonValue) {

}