package com.besok.server.flow.json

import akka.http.scaladsl.model.{HttpMethod, HttpMethods, HttpRequest, Uri}

import scala.io.Source
import org.scalatest.FunSuite

class EndpointTest extends FunSuite {
  implicit val ctx: GeneratorContext = new GeneratorContext()

  test("manager"){
    val yaml = Source.fromResource("endpoints/endpoint.yml").mkString
    val manager = EndpointTemplate.processYaml(yaml)
    val endpoint = manager.findBy(HttpRequest(uri = Uri("/endpoint")))
    assert(endpoint.nonEmpty)
    assert(endpoint.map(e => e.name).exists(_.equals("endpoint1")))

    val endpoint1 = manager.findBy(HttpRequest(uri = Uri("/endpoint/id/param")))
    assert(endpoint1.nonEmpty)
    assert(endpoint1.map(e => e.name).exists(_.equals("endpoint2")))

    assert(ctx.get("_endpoints.endpoint2.input.url.id").equals(StringValue("id")))
    assert(ctx.get("_endpoints.endpoint2.input.url.param").equals(StringValue("param")))

  }

  test("yaml") {
    val yaml = Source.fromResource("endpoints/endpoint.yml").mkString
    val tmpls = EndpointTemplate.processYaml(yaml)
    assert(tmpls.templates.size == 2)
  }

  test("input") {
    val input = Input(HttpMethods.GET, "/abc/abc/{abc}/bcd")
    assert(input.compare("/abc/abc/{abc}/bcd/1").isEmpty)
    assert(input.compare("/abc/abc/bcd/bcd").contains(Map("abc" -> "bcd")))
  }
}
