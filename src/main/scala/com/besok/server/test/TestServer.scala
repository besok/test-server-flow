package com.besok.server.test

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Sink

import scala.concurrent.ExecutionContextExecutor

object TestServer {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {
    Http()
      .bind(interface = "localhost", 9000)
      .to(Sink.foreach(_.handleWithSyncHandler(
        {
          r:HttpRequest =>
            println(s"request: $r")
            HttpResponse()
        }
      )))
      .run()
  }
}
