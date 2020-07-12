package com.besok.server.flow.json.parser

import scala.util.{Failure, Success}


object Handler {

  import JsonParser._
  import GeneratorParser._

  def retrieveJsonFrom(input: String, prefix: String)(implicit ctx: GeneratorContext): Json = {
    retrieveJson(input.intoJson, prefix + "|")
  }

  private def retrieveJson(json: Json, prefix: String)(implicit ctx: GeneratorContext): Json = {
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

  def generateJson(json: Json): Json = json match {
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
