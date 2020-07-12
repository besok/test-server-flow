package com.besok.server.flow.json.parser

import org.scalatest.FunSuite

class HandlerTest extends FunSuite {

  implicit val ctx:GeneratorContext = new GeneratorContext()

  test("test"){
    import scala.io.Source

    val txt = Source.fromResource("jsons/simple_with_gen.json").mkString
    val templateJson = Handler.retrieveJsonFrom(txt, ">")

    assert(templateJson.query("customer.id").get.f.nonEmpty)
    assert(templateJson.query("customer.name").get.f.nonEmpty)
    assert(templateJson.query("customer.amount").get.f.nonEmpty)
    assert(templateJson.query("customer.address.street").get.f.isEmpty)
    assert(templateJson.query("customer.address.geo").get.f.nonEmpty)

    assert(Handler.generateJson(templateJson).query("customer.id").get == IntValue(1))
    assert(Handler.generateJson(templateJson).query("customer.id").get == IntValue(2))
    assert(Handler.generateJson(templateJson).query("customer.id").get == IntValue(3))

    val uuid = Handler.generateJson(templateJson).query("customer.address.geo").get
    assert(ctx.get("geo.address") == uuid)

  }

}
