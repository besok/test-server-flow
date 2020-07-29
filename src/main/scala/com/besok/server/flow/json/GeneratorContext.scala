package com.besok.server.flow.json

import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

trait GeneratorContext {

  def get(key: String): Json

  def put(key: String, value: Json): Option[Json]
}


class GeneratorContextActor extends GeneratorContext {

  override def get(key: String): Json = ???

  override def put(key: String, value: Json): Option[Json] = ???

}

object GeneratorContextActor {
  sealed trait CtxEvent

  case class GetCtxEvent(k: String, sender: ActorRef[CtxEvent]) extends CtxEvent

  case class PutCtxEvent(k: String, v: Json, sender: ActorRef[CtxEvent]) extends CtxEvent

  case class RespondCtxEvent(v: Option[Json]) extends CtxEvent

  class Storage {
    private val ctx = new GeneratorContextMap

    def process: Behaviors.Receive[CtxEvent] = Behaviors.receiveMessage {
      case GetCtxEvent(k, sender) =>
        sender ! RespondCtxEvent(Some(ctx.get(k)))
        Behaviors.same
      case PutCtxEvent(k, v, sender) =>
        sender ! RespondCtxEvent(ctx.put(k, v))
        Behaviors.same
      case _ => Behaviors.same
    }
  }
}

class GeneratorContextMap extends GeneratorContext {
  private val objects = new ConcurrentHashMap[String, Json]()

  def get(key: String): Json = {
    Option(objects.get(key)).getOrElse(NullValue)
  }

  def put(key: String, value: Json): Option[Json] = Option(
    objects.put(key, if (value == null) NullValue else value)
  )
}
