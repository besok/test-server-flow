package com.besok.server.flow.json.parser

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.besok.server.flow.json.parser.Json._

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.{Random, Using}


trait GeneratorContext {
  def get(key: String): Json

  def put(key: String, value: Json): Option[Json]
}

sealed trait GeneratorFunction {
  def generate: Json
}

case class SequenceF(var idx: Int) extends GeneratorFunction {
  override def generate: Json = {
    idx += 1
    (idx - 1).toJson
  }
}

case class StringF(len: Int) extends GeneratorFunction {
  val r: Random = new Random

  override def generate: Json =
    r.alphanumeric
      .filter(_.isLetterOrDigit)
      .take(len)
      .mkString
      .toJson

}

case class NumberF(start: Int, end: Int) extends GeneratorFunction {
  var r: Random = new Random
  if(start >= end){
    throw new JsonException
  }
  override def generate: Json = (start + r.nextInt(end - start)).toJson
}


case class FromListF[T: ClassTag](values: Seq[T]) extends GeneratorFunction {
  private val r: Random = new Random
  private val len = values.size

  override def generate: Json = values(r.nextInt(len)).toJson
}

abstract class FromFile[T: ClassTag](p: String, s: Char, m: String => T) extends GeneratorFunction {

  private val fromListF: FromListF[T] = FromListF {
    Using(Source.fromFile(p)) {
      _.mkString.split(s).map(_.trim).map(m)
    }.get
  }

  override def generate: Json = fromListF.generate
}

case class StringFromFileF(p: String, s: Char) extends FromFile[String](p, s, r => r)

case class IntFromFileF(p: String, s: Char) extends FromFile[Int](p, s, _.toInt)

case class UuidF() extends GeneratorFunction {
  override def generate: Json = UUID.randomUUID.toString.toJson
}

case class DateF(f: String) extends GeneratorFunction {
  override def generate: Json = LocalDateTime.now().format(DateTimeFormatter.ofPattern(f)).toJson
}

case class ArrayF(f: GeneratorFunction, size: Int) extends GeneratorFunction {
  override def generate: Json = (for (_ <- 1 to size) yield f.generate).toJson
}

case class FromContextF(ctx: GeneratorContext, f: GeneratorContext => Json) extends GeneratorFunction {
  override def generate: Json = f(ctx)
}