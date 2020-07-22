package com.besok.server.flow.json

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import org.yaml.snakeyaml.Yaml

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Success, Using}

class EndpointException(s: String) extends RuntimeException(s)

case class Param(name: String, dyn: Boolean)

case class Input(method: HttpMethod, url: String) {

  val params: Seq[Param] = url.split("/").map(_.trim).map {
    p =>
      if (p.startsWith("{")) Param(getP(p), dyn = true)
      else Param(p, dyn = false)
  }

  private def getP(p: String) = {
    p.stripPrefix("{").stripSuffix("}").trim
  }

  def compare(input: String): Option[Map[String, String]] = {
    val inParams = input.split("/")
    if (inParams.length == params.length) {
      Some(
        (for ((in, p) <- inParams.zip(params)) yield {
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

  var generator: JsonGenerator = JsonGenerator(defineInput, prefix)

  def generateJson = generator.newJson

  def stringJson = generateJson.toPrettyString

  private def defineInput: String =
    if (body.startsWith("file:"))
      Using(scala.io.Source.fromFile(body.stripPrefix("file:")))(_.mkString).get
    else body

}

case class EndpointTemplate(name: String, input: Input, output: Output)

object EndpointManager {

  def apply(input: String)(implicit ctx: GeneratorContext): EndpointManager =
    EndpointManager(
      new Yaml()
        .loadAll(input)
        .asScala
        .map(fromYaml)
        .toSeq,
      ctx)

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
      Input(method(get(input, "method")), get(input, "url")),
      Output(get(output, "code"), get(output, "body"), prefix)
    )
  }

  def method(s: String): HttpMethod = s.toLowerCase match {
    case "post" => HttpMethods.POST
    case "get" => HttpMethods.GET
    case "delete" => HttpMethods.DELETE
    case "put" => HttpMethods.PUT
    case _ => throw new EndpointException("the http method does not exist ")
  }

  private def get[T](m: mutable.Map[String, Object], k: String): T = {
    m.getOrElse(k, throw new EndpointException(s"the param $k should exist")).asInstanceOf[T]
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
      .groupBy(_.input.params.length).values
      .foreach { s =>
        for (l <- s; r <- s if r != l && l.input.method == r.input.method) {
          compareInputs(l, r)
        }
      }
  }

  private def compareInputs(left: EndpointTemplate, right: EndpointTemplate): Unit = {
    for (pair <- left.input.params.zip(right.input.params)) {
      pair match {
        case (Param(nl, false), Param(rl, false)) => if (!nl.equals(rl)) return
        case _ => ()
      }
    }
    throw new EndpointException(s"params: ${left.name} and ${right.name} has the equality regarding url")
  }
}

object Endpoints {

  import JsonParser._

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val ctx: GeneratorContext = new GeneratorContext

  def main(args: Array[String]): Unit = {
    val yaml = scala.io.Source.fromFile("C:\\projects\\test-server-flow\\src\\test\\resources\\endpoints\\endpoint.yml").mkString
    setup(EndpointManager(yaml), 8090)
  }

  def setup(m: EndpointManager, port: Int): Unit = {
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
            .andThen { case Success(value) => ctx.put(s"_endpoints.$n.input.body", value) }
          HttpResponse(o.code, entity = HttpEntity(ContentTypes.`application/json`, o.stringJson))
        case None =>
          r.discardEntityBytes()
          HttpResponse(404, entity = "Unknown resource!")
      }
  }

}
