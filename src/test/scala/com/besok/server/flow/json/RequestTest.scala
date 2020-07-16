package com.besok.server.flow.json

import org.scalatest.FunSuite
class RequestTest extends FunSuite {
  implicit val ctx:GeneratorContext = new GeneratorContext()
  test("yaml"){
    import scala.io.Source
    val yaml = Source.fromResource("endpoints/endpoint.yml").mkString

    val tmpls = EndpointTemplate.processYaml(yaml)
    assert(tmpls.size == 2)
  }
}
