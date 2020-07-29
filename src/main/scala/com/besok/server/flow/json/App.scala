package com.besok.server.flow.json

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object App {

  import JsonParser._

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val ctx: GeneratorContext = new GeneratorContextMap

  def main(args: Array[String]): Unit = {
    val yaml = scala.io.Source.fromFile("C:\\projects\\test-server-flow\\src\\test\\resources\\endpoints\\endpoint.yml").mkString
    setupEndpoints(EndpointManager(yaml), 8090)
  }

  def setupEndpoints(m: EndpointManager, port: Int): Unit = {
    Http()
      .bind(interface = "localhost", port)
      .to(Sink.foreach(_ handleWithSyncHandler createEndPoints(m)))
      .run()
  }

  private def createEndPoints(m: EndpointManager) = {
    r: HttpRequest =>
      m.findBy(r) match {
        case Some(EndpointTemplate(n, _, o)) =>
          Unmarshal(r.entity).to[String]
            .map(_.intoJson)
            .foreach {
              case Success(value) => ctx.put(s"_endpoints.$n.input.body", value)
              case Failure(exception) => throw JsonException(s"unable to parse json for endpoint $n", exception)
            }
          HttpResponse(o.code, entity = HttpEntity(ContentTypes.`application/json`, o.stringJson))
        case None =>
          r.discardEntityBytes()
          HttpResponse(404, entity = "Unknown resource!")
      }
  }

  private def createParcels(p: Parcels) = {
    p.triggers
  }
}
