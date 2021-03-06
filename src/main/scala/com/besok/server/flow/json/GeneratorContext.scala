package com.besok.server.flow.json

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.scalalogging.Logger

import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * The context to store all parameters incoming and generating
 */
trait GeneratorContext {

  def get(key: String): Json

  def put(key: String, value: Json): Json

  def putMap(map: Map[String, Json]): Unit
}

class GeneratorContextMap extends GeneratorContext {
  private val objects = new ConcurrentHashMap[String, Json]()
  val log: Logger = Logger[GeneratorContextMap]

  def get(key: String): Json = {
    Option(objects.get(key)).getOrElse(NullValue)
  }

  def put(key: String, value: Json): Json = {
    log.debug(s"put to map, key:$key, value:$value")
    objects.put(key, if (value == null) NullValue else value)
  }

  override def putMap(map: Map[String, Json]) =
    for ((k, v) <- map) {
      put(k, v)
    }
}


sealed trait CtxEvent

case class GetCtxEvent(k: String) extends CtxEvent

case class PutCtxEvent(k: String, v: Json) extends CtxEvent

case class PutMapCtxEvent(pair: Map[String, Json]) extends CtxEvent

case class RespondCtxEvent(v: Json) extends CtxEvent

class GeneratorContextActor extends Actor {

  val storage = new GeneratorContextMap

  override def receive: Receive = {
    case GetCtxEvent(k) => sender() ! RespondCtxEvent(storage.get(k))
    case PutCtxEvent(k, v) => sender() ! RespondCtxEvent(storage.put(k, v))
    case PutMapCtxEvent(map) => storage.putMap(map)
  }
}

class GeneratorContextActorProxy(delegate: ActorRef)(implicit execCtx: ExecutionContext) extends GeneratorContext {
  implicit val timeout: Timeout = Timeout(3.seconds)

  override def get(key: String): Json =
    processFuture(delegate ? GetCtxEvent(key))


  private def processFuture(future: Future[Any]) = {
    Await.result(future, timeout.duration).asInstanceOf[RespondCtxEvent].v
  }

  override def put(key: String, value: Json): Json = processFuture(delegate ? PutCtxEvent(key, value))

  override def putMap(map: Map[String, Json]): Unit = delegate ? PutMapCtxEvent(map)
}
