package com.besok.server.flow.json

import org.scalatest.FunSuite

import scala.io.Source

class ParcelManagerTest extends FunSuite {

  implicit val ctx:GeneratorContext = new GeneratorContext

  test("trigger") {
    assert(EndpointTrigger("name") == Trigger("endpoint(name)"))
    assert(EndpointTrigger("name") == Trigger(" endpoint ( name ) "))
    assert(ParcelTrigger("name") == Trigger(" parcel ( name ) "))
    assert(EverySecTrigger(100,100) == Trigger("every_sec ( 100 , 100 ) "))
    assert(EverySecTrigger(100,0) == Trigger("every_sec (100) "))
    assert(EverySecTrigger(1,0) == Trigger("every_sec() "))
    assert(TimesTrigger(1,0,0) == Trigger("times() "))
    assert(TimesTrigger(101,0,0) == Trigger("times(101) "))
    assert(TimesTrigger(10,1,0) == Trigger("times(10,1) "))
    assert(TimesTrigger(10,1,10) == Trigger("times(10,1,10) "))
  }

  test("file"){
    val yaml = Source.fromResource("endpoints/senders.yml").mkString
    val m = ParcelManager(yaml)

    assert(m.parcels.length == 3)

  }
}
