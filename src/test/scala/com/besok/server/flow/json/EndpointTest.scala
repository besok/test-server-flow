package com.besok.server.flow.json

import scala.io.Source
import org.scalatest.FunSuite

class EndpointTest extends FunSuite {
  implicit val ctx: GeneratorContext = new GeneratorContext()

  test("yaml") {
    val yaml = Source.fromResource("endpoints/endpoint.yml").mkString
    val tmpls = EndpointTemplate.processYaml(yaml)
    assert(tmpls.size == 2)
  }
}
