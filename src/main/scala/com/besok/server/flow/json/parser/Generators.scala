package com.besok.server.flow.json.parser

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.besok.server.flow.json.parser.Json._
import org.parboiled2.{Parser, ParserInput}

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.{Random, Using}

class GeneratorContext {
  type Key = String
  private val objects = new ConcurrentHashMap[Key, Json]()

  def get(key: String): Json = {
    Option(objects.get(key)).getOrElse(NullValue)
  }

  def put(key: String, value: Json): Option[Json] = Option(
    objects.put(key, if (value == null) NullValue else value)
  )
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
  if (start >= end) {
    throw new JsonException
  }

  override def generate: Json = (start + r.nextInt(end - start)).toJson
}


case class FromListF[T: ClassTag](values: Seq[T]) extends GeneratorFunction {
  private val r: Random = new Random
  private val len = values.size

  override def generate: Json = values(r.nextInt(len)).toJson
}

abstract class FromFile[T: ClassTag](p: String, s: String, m: String => T) extends GeneratorFunction {

  private val fromListF: FromListF[T] = FromListF {
    Using(Source.fromFile(p)) {
      _.mkString.split(s).map(_.trim).map(m)
    }.get
  }

  override def generate: Json = fromListF.generate
}

case class StringFromFileF(p: String, s: String) extends FromFile[String](p, s, r => r)

case class IntFromFileF(p: String, s: String) extends FromFile[Int](p, s, _.toInt)

case class UuidF() extends GeneratorFunction {
  override def generate: Json = UUID.randomUUID.toString.toJson
}

case class DateF(f: String) extends GeneratorFunction {
  override def generate: Json = LocalDateTime.now().format(DateTimeFormatter.ofPattern(f)).toJson
}

case class ArrayF(f: GeneratorFunction, size: Int) extends GeneratorFunction {
  override def generate: Json = (for (_ <- 1 to size) yield f.generate).toJson
}

case class WithCtxF(f: GeneratorFunction, key:String)(implicit ctx: GeneratorContext) extends GeneratorFunction {
  override def generate: Json = {
    val value = f.generate
    ctx.put(key,value)
    value
  }
}

case class FromContextF(ctx: GeneratorContext, f: GeneratorContext => Json) extends GeneratorFunction {

  override def generate: Json = f(ctx)
}

object FromContextF {
  def byName(name: String) = (_: GeneratorContext).get(name)
}

class GeneratorParser(val input: ParserInput)(implicit val currCtx: GeneratorContext) extends Parser {

  import org.parboiled2._
  import CharPredicate.Digit

  val WhiteSpaceChar: CharPredicate = CharPredicate(" \n\r\t\f")
  val STR: CharPredicate = CharPredicate.All -- ',' -- ')' -- "\""
  val CtxStr: CharPredicate = CharPredicate.All -- ',' -- ')' -- "\"" -- "[]"


  def sp(c: String) = rule(zeroOrMore(WhiteSpaceChar) ~ c ~ zeroOrMore(WhiteSpaceChar))

  def name(funcName: String) = rule {
    zeroOrMore(WhiteSpaceChar) ~ funcName ~ sp("(")
  }

  def posNum = rule {
    capture(oneOrMore(Digit)) ~> (_.toInt)
  }

  def parseFromFile = rule {
    internalString ~ optional(sp(",") ~ internalString) ~ sp(")")
  }

  def internalString = rule {
    capture(oneOrMore(STR))
  }

  def funcSequence = rule {
    name("seq") ~ posNum ~ sp(")") ~> SequenceF
  }

  def funcString = rule {
    name("str") ~ posNum ~ sp(")") ~> StringF
  }

  def funcNumber = rule {
    name("num") ~ posNum ~ sp(",") ~ posNum ~ sp(")") ~> NumberF
  }

  def funcStrFromFile = rule {
    name("str_file") ~ parseFromFile ~>
      ((p: String, s: Option[String]) => StringFromFileF(p, s.getOrElse(",")))
  }

  def funcIntFromFile = rule {
    name("num_file") ~ parseFromFile ~>
      ((p: String, s: Option[String]) => IntFromFileF(p, s.getOrElse(",")))
  }


  def funcStrFromList: Rule1[FromListF[String]] = rule {
    name("str_list") ~ zeroOrMore(internalString).separatedBy(sp(",")) ~ sp(")") ~> ((v: Seq[String]) => FromListF(v))
  }

  def funcNumFromList: Rule1[FromListF[Int]] = rule {
    name("num_list") ~ zeroOrMore(internalString).separatedBy(sp(",")) ~ sp(")") ~> ((v: Seq[String]) => FromListF(v.map(_.trim).map(_.toInt)))
  }

  def funcUUID: Rule1[UuidF] = rule {
    capture(name("uuid")) ~ sp(")") ~> ((_: String) => UuidF())
  }

  def funcDate = rule {
    name("time") ~ optional(internalString) ~ sp(")") ~> ((v: Option[String]) => v match {
      case Some(value) => DateF(value)
      case None => DateF(new SimpleDateFormat().toPattern)
    })
  }

  def funcCtx = rule {
    name("from_ctx") ~ internalString ~ sp(")") ~> (v => FromContextF(currCtx, _.get(v)))
  }

  def funcArray:Rule1[ArrayF] = rule {
    name("array") ~ posNum ~ sp(",") ~ function ~ sp(")") ~> ((n:Int, f:GeneratorFunction) => ArrayF(f,n))
  }

  def function = rule {
      funcSequence |
      funcCtx |
      funcDate |
      funcUUID |
      funcNumFromList |
      funcStrFromList |
      funcIntFromFile |
      funcStrFromFile |
      funcNumber |
      funcString |
      funcSequence |
      funcArray
  }

  def functionWithCallback: Rule1[GeneratorFunction] = rule{
    function ~ optional(sp( "=>") ~ ctxVar) ~> ((f:GeneratorFunction, v:Option[String]) => v match {
     case None => f
     case Some(vr) => WithCtxF(f,vr)
   })
  }

  def ctxVar = rule {
    sp("[") ~ capture(oneOrMore(CtxStr)) ~ sp("]")
  }


}

object GeneratorParser {

  implicit def stringToParser(input: String)(implicit ctx:GeneratorContext) = new GeneratorParser(input)(ctx)
}
