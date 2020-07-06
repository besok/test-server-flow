package com.besok.server.flow.json.parser


sealed trait GeneratorFunction {
  def generate(ctx: GeneratorContext): Json
}

class GeneratorContext

class Generator(f: GeneratorFunction, jv: Json) {

}