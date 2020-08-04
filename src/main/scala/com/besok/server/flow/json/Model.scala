package com.besok.server.flow.json

import akka.http.scaladsl.model._
import com.typesafe.scalalogging.Logger
import org.yaml.snakeyaml.Yaml

import scala.collection.mutable
import scala.jdk.CollectionConverters._

class EndpointException(s: String) extends RuntimeException(s)

case class Param(name: String, dyn: Boolean)

case class StringUrl(url: String) {
  val params: Seq[Param] = url.split("/").map(_.trim).map {
    p =>
      if (p.startsWith("{")) Param(getP(p), dyn = true)
      else Param(p, dyn = false)
  }
  private def getP(p: String) = {
    p.stripPrefix("{").stripSuffix("}").trim
  }


}


case class Input(method: HttpMethod, url: StringUrl) {
  def compare(input: String): Option[Map[String, String]] = {
    val inParams = input.split("/")
    if (inParams.length == url.params.length) {
      Some(
        (for ((in, p) <- inParams.zip(url.params)) yield {
          p match {
            case Param(n, false) => if (!n.equals(in)) return None else ("", "")
            case Param(n, true) => (n, in)
          }
        }).toMap
      )
    } else {
      None
    }
  }
}

case class Output(code: Int, body: String, prefix: String)(implicit ctx: GeneratorContext) {

  var generator: JsonGenerator = JsonGenerator.fromFile(body, prefix)

  def stringJson = generator.newJsonString

}

case class EndpointTemplate(name: String, input: Input, output: Output)

object EndpointManager {

  import YamlHelper._

  def apply(input: String)(implicit ctx: GeneratorContext): EndpointManager =
    EndpointManager(load(input).map(fromYaml).filter(_.nonEmpty).map(_.get).toSeq, ctx)

  def fromYaml(params: AnyRef)(implicit ctx: GeneratorContext): Option[EndpointTemplate] = {

    val map = params.asInstanceOf[java.util.LinkedHashMap[String, Object]].get("endpoint")
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    if (map == null) {
      return None
    }
    val input = map.getOrElse("input", throw new EndpointException(s"the input should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val output = map.getOrElse("output", throw new EndpointException(s"the output should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala

    val prefix: String = output.getOrElse("prefix", ">").asInstanceOf[String]

    Some(
      EndpointTemplate(
        get(map, "name"),
        Input(method(get(input, "method")), StringUrl(get(input, "url"))),
        Output(get(output, "code"), get(output, "body"), prefix)
      ))
  }
}

case class EndpointManager(templates: Seq[EndpointTemplate], ctx: GeneratorContext) {

  import Json._

  checkDuplicates

  def findBy(r: HttpRequest): Option[EndpointTemplate] = r match {
    case HttpRequest(m, Uri.Path(p), _, _, _) =>
      for (t <- templates if t.input.method == m) {
        t.input.compare(p) match {
          case Some(m) =>
            ctx.putMap(m.map { case (k, v) => (s"_endpoints.${t.name}.input.url.${k}", v.toJson) })
            return Some(t)
          case None => ()
        }
      }
      None
  }

  private def checkDuplicates: Unit = {
    templates
      .groupBy(_.input.url.params.length).values
      .foreach { s =>
        for (l <- s; r <- s if r != l && l.input.method == r.input.method) {
          compareInputs(l, r)
        }
      }
  }

  private def compareInputs(left: EndpointTemplate, right: EndpointTemplate): Unit = {
    for (pair <- left.input.url.params.zip(right.input.url.params)) {
      pair match {
        case (Param(nl, false), Param(rl, false)) => if (!nl.equals(rl)) return
        case _ => ()
      }
    }
    throw new EndpointException(s"params: ${left.name} and ${right.name} has the equality regarding url")
  }
}


case class Parcel(name: String, receiver: Receiver, message: Message, var trigger: Trigger)(implicit ctx: GeneratorContext) {

  def request = {
    HttpRequest(
      method = receiver.m,
      Uri(url),
      entity = HttpEntity(contentType = ContentTypes.`application/json`, string = message.stringJson.getOrElse(""))
    )
  }

  def url: String =
    receiver.url.params
      .map { p => if (p.dyn) ctx.get(p.name).onlyString else p.name }
      .mkString("/")


}

case class Receiver(m: HttpMethod, url: StringUrl)

case class Message(body: String, prefix: String)(implicit ctx: GeneratorContext) {
  var generator: Option[JsonGenerator] =
    if (body.isEmpty) None else Some(JsonGenerator.fromFile(body, prefix))

  def stringJson: Option[String] = generator.map(_.newJsonString)
}

sealed abstract class Trigger(name: String)

object Trigger {
  def apply(trigger: String): Trigger = trigger.trim.split('(') match {
    case Array(l, r) => l.trim match {
      case "endpoint" => EndpointTrigger(r.stripSuffix(")").trim)
      case "parcel" => ParcelTrigger(r.stripSuffix(")").trim)
      case "times" => processTimeTrigger(r.stripSuffix(")").trim.split(","))
    }

    case _ => throw new EndpointException(s"the  trigger function should be defined decently, but got : $trigger")
  }

  private def processTimeTrigger(args: Array[String]) = {
    if (args.length > 3) {
      throw new EndpointException(s"time trigger should be time(number,gap,delay) but got $args")
    }

    if (args.length == 3)
      TimesTrigger(args(0).trim.toInt, args(1).trim.toInt, args(2).trim.toInt)
    else if (args.length == 2)
      TimesTrigger(args(0).trim.toInt, args(1).trim.toInt, 0)
    else if (args.length == 1) {
      val a = args(0).trim
      if (a.equals("")) TimesTrigger(1, 0, 0) else TimesTrigger(a.toInt, 0, 0)
    } else {
      TimesTrigger(1, 0, 0)
    }
  }
}

case class EndpointTrigger(name: String) extends Trigger("endpoint")

case class ParcelTrigger(name: String) extends Trigger("parcel")

case class TimesTrigger(num: Int, gap: Int, delay: Int) extends Trigger("times")

case class Parcels(parcels: Seq[Parcel]) {
  val triggers: Map[Trigger, Seq[Parcel]] = parcels.groupBy(_.trigger)

  def find(t: Trigger): Seq[Parcel] = triggers.getOrElse(t, Seq())
}

object Parcels {

  import YamlHelper._

  def apply(input: String)(implicit ctx: GeneratorContext): Parcels = {
    Parcels(load(input).map(fromYaml).filter(_.nonEmpty).map(_.get).toSeq)
  }

  def fromYaml(params: AnyRef)(implicit ctx: GeneratorContext): Option[Parcel] = {

    val map = params.asInstanceOf[java.util.LinkedHashMap[String, Object]].get("parcel")
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala

    if (map == null) {
      return None
    }

    val receiver = map.getOrElse("receiver", throw new EndpointException(s"the receiver should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val message = map.get("message")
      .map(_.asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala)

    val mes =
      if (message.isEmpty) Message("", ">")
      else Message(get(message.get, "body"), message.get.getOrElse("prefix", ">").asInstanceOf[String])

    Some(Parcel(
      get(map, "name"),
      Receiver(method(get(receiver, "method")), StringUrl(get(receiver, "url"))),
      mes,
      Trigger(get(map, "trigger"))
    ))
  }
}

object YamlHelper {

  def load(input: String) = new Yaml().loadAll(input).asScala

  def get[T](m: mutable.Map[String, Object], k: String): T = {
    m.getOrElse(k, throw new EndpointException(s"the param $k should exist")).asInstanceOf[T]
  }

  def method(s: String): HttpMethod = s.toLowerCase match {
    case "post" => HttpMethods.POST
    case "get" => HttpMethods.GET
    case "delete" => HttpMethods.DELETE
    case "put" => HttpMethods.PUT
    case _ => throw new EndpointException("the http method does not exist ")
  }
}