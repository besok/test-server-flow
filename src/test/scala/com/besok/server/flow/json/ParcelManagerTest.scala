package com.besok.server.flow.json

import org.scalatest.FunSuite

import scala.io.Source

class ParcelManagerTest extends FunSuite {

  implicit val ctx: GeneratorContext = new GeneratorContextMap

  test("trigger") {
    assert(EndpointTrigger("name") == Trigger("endpoint(name)"))
    assert(EndpointTrigger("name") == Trigger(" endpoint ( name ) "))
    assert(ParcelTrigger("name") == Trigger(" parcel ( name ) "))
    assert(EverySecTrigger(100, 100) == Trigger("every_sec ( 100 , 100 ) "))
    assert(EverySecTrigger(100, 0) == Trigger("every_sec (100) "))
    assert(EverySecTrigger(1, 0) == Trigger("every_sec() "))
    assert(TimesTrigger(1, 0, 0) == Trigger("times() "))
    assert(TimesTrigger(101, 0, 0) == Trigger("times(101) "))
    assert(TimesTrigger(10, 1, 0) == Trigger("times(10,1) "))
    assert(TimesTrigger(10, 1, 10) == Trigger("times(10,1,10) "))
  }

  test("file") {
    val yaml = Source.fromResource("endpoints/senders.yml").mkString
    val m = Parcels(yaml)

    assert(m.parcels.length == 3)

  }

  test("parcel") {
    ctx.put("_endpoints.endpoint2.input.url.id", IntValue(42))
    val p =
      """parcel:
        |  name: parcel2
        |  receiver:
        |    method: get
        |    url: 'http://127.0.0.1:9000/api/parcel2/{_endpoints.endpoint2.input.url.id}/send'
        |  message:
        |    body: '{">|id":"uuid() => [p2.id]"}'
        |  trigger: every_sec(1)""".stripMargin

    val parcel = Parcels(p).parcels.head
    val url = parcel.url
    assert(url.equals("http://127.0.0.1:9000/api/parcel2/42/send"))
  }


}
