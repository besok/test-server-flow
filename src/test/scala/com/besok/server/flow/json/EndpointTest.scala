package com.besok.server.flow.json

import akka.http.scaladsl.model.{HttpMethod, HttpMethods, HttpRequest, Uri}

import scala.io.Source
import org.scalatest.FunSuite

class EndpointTest extends FunSuite {
  implicit val ctx: GeneratorContext = new GeneratorContext()

  test("manager") {
    val yaml = Source.fromResource("endpoints/endpoint.yml").mkString
    val manager = EndpointManager(yaml)
    val endpoint = manager.findBy(HttpRequest(method = HttpMethods.POST, uri = Uri("/endpoint")))
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
    val tmpls = EndpointManager(yaml)
    assert(tmpls.templates.size == 2)
  }

  test("input") {
    val input = Input(HttpMethods.GET, StringUrl("/abc/abc/{abc}/bcd"))
    assert(input.compare("/abc/abc/{abc}/bcd/1").isEmpty)
    assert(input.compare("/abc/abc/bcd/bcd").contains(Map("" -> "", "abc" -> "bcd")))
  }

  test("duplicate") {
    assertThrows[EndpointException] {
      EndpointManager(
        """---
          |endpoint:
          |  name: endpoint2
          |  input:
          |    method: post
          |    url: /endpoint/path/api
          |  output:
          |    code: 200
          |    body: '{}'
          |---
          |endpoint:
          |  name: endpoint1
          |  input:
          |    method: post
          |    url: /endpoint/{id}/{param}
          |  output:
          |    code: 200
          |    body: '{}'
          |
          |""".stripMargin)
    }
    EndpointManager(
      """---
        |endpoint:
        |  name: endpoint2
        |  input:
        |    method: get
        |    url: /endpoint/path/api
        |  output:
        |    code: 200
        |    body: '{}'
        |---
        |endpoint:
        |  name: endpoint1
        |  input:
        |    method: post
        |    url: /endpoint/{id}/{param}
        |  output:
        |    code: 200
        |    body: '{}'
        |
        |""".stripMargin)
    EndpointManager(
      """---
        |endpoint:
        |  name: endpoint2
        |  input:
        |    method: post
        |    url: /endpoint/path/api
        |  output:
        |    code: 200
        |    body: '{}'
        |
        |""".stripMargin)
  }
}