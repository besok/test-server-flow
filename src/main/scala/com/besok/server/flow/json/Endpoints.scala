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

case class Input(method: HttpMethod, url: String) {

  case class Param(name: String, dyn: Boolean)

  val params: Seq[Param] = url.split("/").map(_.trim).map {
    p => {
      if (p.startsWith("{"))
        Param(p.stripPrefix("{").stripSuffix("}").trim, dyn = true)
      else Param(p, dyn = false)
    }
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

  private def defineInput: String =
    if (body.startsWith("file:"))
      Using(scala.io.Source.fromFile(body.stripPrefix("file:")))(_.mkString).get
    else body

}

case class EndpointTemplate(name: String, input: Input, output: Output)

object EndpointTemplate {

  def processYaml(input: String)(implicit ctx: GeneratorContext): EndpointTemplateList =
    EndpointTemplateList(new Yaml().loadAll(input).asScala.map(fromYaml).toSeq, ctx)

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

case class EndpointTemplateList(templates: Seq[EndpointTemplate], ctx: GeneratorContext) {

  import Json._

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


}

object Endpoints {

  import JsonParser._

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val ctx: GeneratorContext = new GeneratorContext

  def main(args: Array[String]): Unit = {
    val yaml = scala.io.Source.fromFile("C:\\projects\\test-server-flow\\src\\test\\resources\\endpoints\\endpoint.yml").mkString

    setup(EndpointTemplate.processYaml(yaml), 8090)
  }

  def setup(m: EndpointTemplateList, port: Int): Unit = {
    val serverSource = Http().bind(interface = "localhost", port)
    val requestHandler: HttpRequest => HttpResponse = {
      r: HttpRequest =>
        m.findBy(r) match {
          case Some(EndpointTemplate(n, _, o)) =>
            Unmarshal(r.entity).to[String]
              .map(_.intoJson)
              .andThen { case Success(value) => ctx.put(s"_endpoints.$n.input.body", value) }
            HttpResponse(
              status = o.code,
              entity = HttpEntity(contentType = ContentTypes.`application/json`, o.generateJson.toPrettyString)
            )
          case None =>
            r.discardEntityBytes()
            HttpResponse(404, entity = "Unknown resource!")
        }
    }
    val bindingFuture: Future[Http.ServerBinding] =
      serverSource.to(
        Sink.foreach { connection =>
          connection handleWithSyncHandler requestHandler
        }
      ).run()
  }

  private def notFound(r: HttpRequest) = {
    r.discardEntityBytes()
    HttpResponse(404, entity = "Unknown resource!")
  }
}
