package com.besok.server.flow.json

import org.yaml.snakeyaml.Yaml

import scala.collection.mutable
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Using

class EndpointException(s: String) extends RuntimeException(s)

sealed class Method

object Method {
  def from(s: String): Method = s.toLowerCase match {
    case "post" => POST
    case "get" => GET
    case "delete" => DELETE
    case "put" => PUT
    case _ => throw new EndpointException("the http method does not exist ")
  }
}

case object POST extends Method

case object GET extends Method

case object DELETE extends Method

case object PUT extends Method


case class Input(method: Method, url: String)

case class Output(code: Int, body: String, prefix: String = ">")(implicit ctx: GeneratorContext) {

  var generator: JsonGenerator = JsonGenerator(defineInput, prefix)

  def generateJson = generator.newJson

  private def defineInput: String =
    if (body.startsWith("file:"))
      Using(Source.fromFile(body.stripPrefix("file:")))(_.mkString).get
    else body

}

case class EndpointTemplate(name: String, input: Input, output: Output)

object EndpointTemplate {

  def processYaml(input: String)(implicit ctx: GeneratorContext): Seq[EndpointTemplate] =
    new Yaml()
      .loadAll(input)
      .asScala.map(i => fromYaml(i.asInstanceOf[java.util.LinkedHashMap[String, Object]]))
      .toSeq

  def fromYaml(params: java.util.LinkedHashMap[String, Object])(implicit ctx: GeneratorContext): EndpointTemplate = {
    val map = params.get("endpoint").asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val input = map.getOrElse("input", throw new EndpointException(s"the input should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala
    val output = map.getOrElse("output", throw new EndpointException(s"the output should exist"))
      .asInstanceOf[java.util.LinkedHashMap[String, Object]].asScala

    val prefix: String = output.getOrElse("prefix", ">").asInstanceOf[String]

    EndpointTemplate(
      get(map, "name"),
      Input(Method.from(get(input, "method")), get(input, "url")),
      Output(get(output, "code"), get(output, "body"), prefix)
    )
  }

  private def get[T](m: mutable.Map[String, Object], k: String): T = {
    m.getOrElse(k, throw new EndpointException(s"the param $k should exist")).asInstanceOf[T]
  }
}


