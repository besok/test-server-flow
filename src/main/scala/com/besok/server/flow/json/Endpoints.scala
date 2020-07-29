package com.besok.server.flow.json

import akka.http.scaladsl.model._
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
    EndpointManager(load(input).map(fromYaml).toSeq, ctx)

  def fromYaml(params: AnyRef)(implicit ctx: GeneratorContext): EndpointTemplate = {

    val map = params.asInstanceOf[java.util.LinkedHashMap[String, Object]].get("endpoint")
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val input = map.getOrElse("input", throw new EndpointException(s"the input should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val output = map.getOrElse("output", throw new EndpointException(s"the output should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala

    val prefix: String = output.getOrElse("prefix", ">").asInstanceOf[String]

    EndpointTemplate(
      get(map, "name"),
      Input(method(get(input, "method")), StringUrl(get(input, "url"))),
      Output(get(output, "code"), get(output, "body"), prefix)
    )
  }
}

case class EndpointManager(templates: Seq[EndpointTemplate], ctx: GeneratorContext) {

  import Json._

  checkDuplicates

  def findBy(r: HttpRequest): Option[EndpointTemplate] = r match {
    case HttpRequest(m, Uri.Path(p), _, _, _) =>
      for (t <- templates if t.input.method == m) {
        t.input.compare(p) match {
          // TODO: change the plain keys to the object since it can be parallel(no lock now)
          case Some(m) =>
            m.foreach { case (k, v) => ctx.put(s"_endpoints.${t.name}.input.url.${k}", v.toJson) }
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