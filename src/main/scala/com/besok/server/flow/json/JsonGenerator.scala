package com.besok.server.flow.json

import scala.util.{Failure, Success}


case class JsonGenerator(inputTemplate: String, prefix: String = ">")(implicit ctx: GeneratorContext) {

  import JsonParser._
  import GeneratorParser._

  val template: Json = retrieveJsonFrom(inputTemplate, prefix)

  def newJson = generateJson(template)

  private def retrieveJsonFrom(input: String, prefix: String): Json = {
    retrieveJson(input.intoJson, prefix + "|")
  }

  private def retrieveJson(json: Json, prefix: String): Json = {
    json match {
      case ObjectValue(kv) => ObjectValue {
        for ((k, v) <- kv) yield {
          v match {
            case StringValue(fun) => if (k.startsWith(prefix)) {
              fun.functionWithCallback.run() match {
                case Success(fn) => v.f = Some(fn); (k.stripPrefix(prefix), v)
                case Failure(exception) => throw exception
              }
            } else (k, v)
            case js@ObjectValue(_) => (k, retrieveJson(js, prefix))
            case _ => (k, v)
          }
        }
      }
      case ArrayValue(seq) => ArrayValue {
        for (v <- seq) yield retrieveJson(v, prefix)
      }
      case _ => json
    }
  }

  private def generateJson(json: Json): Json = json match {
    case ObjectValue(v) => ObjectValue {
      for ((k, v) <- v) yield (k, generateJson(v))
    }
    case ArrayValue(v) => ArrayValue {
      for (vl <- v) yield generateJson(vl)
    }
    case js => generatePlainJson(js)
  }

  private def generatePlainJson(js: Json) = js.f match {
    case None => js
    case Some(value) => value.generate
  }

}

object JsonGenerator {
  implicit def stringToJsonHandler(i:String)(implicit ctx:GeneratorContext) = JsonGenerator(i)
}
