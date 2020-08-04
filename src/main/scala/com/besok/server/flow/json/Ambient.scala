package com.besok.server.flow.json

import akka.pattern.ask
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class SendParcelActor extends Actor {
  implicit val actorSystem: ActorSystem = context.system
  val logger: Logger = Logger[SendParcelActor]

  override def receive: Actor.Receive = {
    case SendRequest(p) =>
      send(p)
      sender() ! EventSendRequest(ParcelTrigger(p.name))
    case ScheduledSendRequest(p, curr) =>
      send(p)
      sender() ! ScheduledSendResponse(p, curr - 1)
  }

  def send(p: Parcel) = {
    val request = p.request
    logger.debug(s"send request: $request")
    Http() singleRequest request
  }
}

case class ScheduledParcelActor(parcelSender: ActorRef) extends Actor {
  implicit val ex: ExecutionContextExecutor = context.system.dispatcher
  private val scheduler: Scheduler = context.system.scheduler

  override def receive: Receive = {
    case ScheduledSendRequest(p@Parcel(_, _, _, TimesTrigger(num, _, delay)), _) =>
      scheduler.scheduleOnce(delay.seconds) {
        parcelSender ! ScheduledSendRequest(p, num)
      }
    case ScheduledSendResponse(p@Parcel(_, _, _, TimesTrigger(_, gap, _)), curr) if curr > 0 =>
      scheduler.scheduleOnce(gap.seconds) {
        parcelSender ! ScheduledSendRequest(p, curr)
      }

  }
}

case class ParcelProcessorActor(parcelSender: ActorRef, parcels: Parcels) extends Actor {
  implicit val ex: ExecutionContextExecutor = context.system.dispatcher
  val logger: Logger = Logger[ParcelProcessorActor]
  override def receive: Actor.Receive = {
    case EventSendRequest(t) =>
      parcels.find(t).foreach(parcelSender ! SendRequest(_))

    case InitScheduledTriggers =>
      parcels.triggers
        .filter {
          case (_: TimesTrigger, _) => true
          case _ => false
        }
        .foreach {
          case (TimesTrigger(_, _, _), triggeredParcels) =>
            triggeredParcels.foreach {
              p: Parcel =>
                val ref = context.system.actorOf(Props(ScheduledParcelActor(parcelSender)))
                logger.debug(s" new scheduled parcer actor has been created for parcel:$p")
                ref ! ScheduledSendRequest(p, 0)
            }
        }

  }
}

sealed class ParcelMessage

case class EventSendRequest(t: Trigger) extends ParcelMessage

case class SendRequest(p: Parcel) extends ParcelMessage

case class ScheduledSendRequest(p: Parcel, curr: Int) extends ParcelMessage

case class ScheduledSendResponse(p: Parcel, curr: Int) extends ParcelMessage

case object InitScheduledTriggers extends ParcelMessage

object SystemConfigurer {
  import JsonParser._

  implicit val system: ActorSystem = ActorSystem()

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val ctx: GeneratorContext = setupGeneratorContext()


  def setupGeneratorContext(): GeneratorContext = {
    new GeneratorContextActorProxy(system.actorOf(Props[GeneratorContextActor]))
  }

  def setup(input: String, port: Int): Unit = {
    setupEndpoints(
      EndpointManager(input),
      port,
      setupParcels(Parcels(input), classOf[SendParcelActor])
    )
  }

  def setupEndpoints(m: EndpointManager, port: Int, parcelProcessor: ActorRef)(implicit system: ActorSystem): Unit = {
    Http()
      .bind(interface = "localhost", port)
      .to(Sink.foreach(_ handleWithSyncHandler createEndPoints(m, parcelProcessor)))
      .run()
  }

  def setupParcels(parcels: Parcels, senderClass: Class[_])(implicit system: ActorSystem) = {
    val sender = system.actorOf(Props(senderClass))
    val processor = system.actorOf(Props(ParcelProcessorActor(sender, parcels)))
    processor ? InitScheduledTriggers
    processor
  }

  private def createEndPoints(m: EndpointManager, parcelProcessor: ActorRef)(implicit ctx: GeneratorContext) = {
    r: HttpRequest =>
      m.findBy(r) match {
        case Some(EndpointTemplate(n, _, o)) =>
          Unmarshal(r.entity).to[String]
            .map(_.intoJson)
            .foreach {
              case Success(value) =>
                ctx.put(s"_endpoints.$n.input.body", value)
              case Failure(exception) => throw JsonException(s"unable to parse json for endpoint $n", exception)
            }
          parcelProcessor ? EventSendRequest(EndpointTrigger(n))
          HttpResponse(o.code, entity = HttpEntity(ContentTypes.`application/json`, o.stringJson))
        case None =>
          r.discardEntityBytes()
          HttpResponse(404, entity = "Unknown resource!")
      }
  }
}