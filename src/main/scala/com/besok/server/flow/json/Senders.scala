package com.besok.server.flow.json

import akka.http.scaladsl.model.HttpMethod

import scala.jdk.CollectionConverters._

case class Parcel(name: String, receiver: Receiver, message: Message, trigger: Trigger)

case class Receiver(m: HttpMethod, url: StringUrl)

case class Message(body: String, prefix: String)(implicit ctx: GeneratorContext) {
  var generator: JsonGenerator = JsonGenerator.fromFile(body, prefix)
}

sealed abstract class Trigger(name: String)

object Trigger {
  def apply(trigger: String): Trigger = trigger.trim.split('(') match {
    case Array(l, r) => l.trim match {
      case "endpoint" => EndpointTrigger(r.stripSuffix(")").trim)
      case "parcel" => ParcelTrigger(r.stripSuffix(")").trim)
      case "every_sec" =>
        val args = r.stripSuffix(")").trim.split(",")
        if (args.length > 2) {
          throw new EndpointException(s" the func every_sec should have no more than 2 arg namely number of sec and delay before start but got: $trigger")
        }
        if (args.length == 2) {
          EverySecTrigger(args(0).trim.toInt, args(1).trim.toInt)
        } else if (args.length == 1) {
          val a = args(0).trim
          if (a.equals("")) {
            EverySecTrigger(1, 0)
          } else {
            EverySecTrigger(a.toInt, 0)
          }
        } else {
          EverySecTrigger(0, 0)
        }
      case "times" =>
        val args = r.stripSuffix(")").trim.split(",")
        if (args.length > 3) {
          throw new EndpointException(s" the func times should have no more than 3 arg namely number of times and gap and delay before start but got: $trigger")
        }
        if (args.length == 3) {
          TimesTrigger(args(0).trim.toInt, args(1).trim.toInt, args(2).trim.toInt)
        } else if (args.length == 2) {
          TimesTrigger(args(0).trim.toInt, args(1).trim.toInt, 0)
        } else if (args.length == 1 && args.nonEmpty) {
          val a = args(0).trim
          if (a.equals("")) {
            TimesTrigger(1, 0, 0)
          } else {
            TimesTrigger(args(0).trim.toInt, 0, 0)
          }
        } else {
          TimesTrigger(1, 0, 0)
        }
    }

    case _ => throw new EndpointException(s"the  trigger function should be defined decently, but got : $trigger")
  }
}

case class EndpointTrigger(name: String) extends Trigger("endpoint")

case class EverySecTrigger(gap: Int, delay: Int) extends Trigger("every_sec")

case class TimesTrigger(num: Int, gap: Int, delay: Int) extends Trigger("times")

case class ParcelTrigger(name: String) extends Trigger("parcel")

case class ParcelManager(parcels: Seq[Parcel])

object ParcelManager {

  import YamlHelper._

  def apply(input: String)(implicit ctx: GeneratorContext): ParcelManager = {
    ParcelManager(load(input).map(fromYaml).toSeq)
  }

  def fromYaml(params: AnyRef)(implicit ctx: GeneratorContext): Parcel = {

    val map = params.asInstanceOf[java.util.LinkedHashMap[String, Object]].get("parcel")
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val receiver = map.getOrElse("receiver", throw new EndpointException(s"the receiver should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val message = map.getOrElse("message", throw new EndpointException(s"the message should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val prefix: String = message.getOrElse("prefix", ">").asInstanceOf[String]

    Parcel(
      get(map, "name"),
      Receiver(method(get(receiver, "method")), StringUrl(get(receiver, "url"))),
      Message(get(message, "body"), prefix),
      Trigger(get(map, "trigger"))
    )
  }


}