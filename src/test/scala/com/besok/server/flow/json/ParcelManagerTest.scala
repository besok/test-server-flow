package com.besok.server.flow.json

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import com.besok.server.flow.json.Test.SimpleSendParcelActor
import org.scalatest.FunSuite

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.Source

class ParcelManagerTest extends FunSuite {

  implicit val ctx: GeneratorContext = new GeneratorContextMap

  test("trigger") {
    assert(EndpointTrigger("name") == Trigger("endpoint(name)"))
    assert(EndpointTrigger("name") == Trigger(" endpoint ( name ) "))
    assert(ParcelTrigger("name") == Trigger(" parcel ( name ) "))
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
        |  trigger: every_sec(1)
        |   """.stripMargin

    val parcel = Parcels(p).parcels.head
    val url = parcel.url
    assert(url.equals("http://127.0.0.1:9000/api/parcel2/42/send"))
  }


  test("actors") {
    implicit val system: ActorSystem = ActorSystem()
    val yaml = Source.fromResource("endpoints/simple_senders.yml").mkString

    SystemConfigurer.setupParcels(Parcels(yaml), classOf[SimpleSendParcelActor])
    Thread.sleep(10000)
    assert(Test.c == 8)
  }
}

object Test {
  var c = 0

  class SimpleSendParcelActor extends SendParcelActor {
    implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher

    override def send(p: Parcel): Future[HttpResponse] = Future {
      c += 1
      HttpResponse()
    }
  }

}