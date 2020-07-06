package com.besok.server.flow.json.parser

import org.parboiled2.{Parser, ParserInput, StringBuilding}

import scala.language.implicitConversions
import scala.util.{Failure, Success}


abstract sealed class Json {
  override def toString: String = this match {
    case NullValue => "null"
    case IntValue(v) => v.toString
    case DoubleValue(v) => v.toString
    case StringValue(v) => "\""+v+"\""
    case BooleanValue(v) => v.toString
    case ArrayValue(arr) => s"""[${arr.mkString(",")}]"""
    case o @ ObjectValue(_) => s"""{${Json.mapToString(o)}}"""

  }
}

object Json {

  implicit class StringHelper(v: String) {
    def toIntValue: IntValue = IntValue(v.toInt)

    def toStringValue: StringValue = StringValue(v)

    def toDoubleValue: DoubleValue = DoubleValue(v.toDouble)

    def toBoolValue: BooleanValue = BooleanValue(v.toBoolean)

    def toNullValue: Json = NullValue
  }

  def toObject(f: Seq[(String, Json)]) = ObjectValue(f.toMap)

  def mapToString(o: ObjectValue) = {
    (for ((k, v) <- o.v) yield {
      s""""$k": ${v.toString}"""
    }).mkString(",")
  }
}

case class StringValue(v: String) extends Json

case class IntValue(v: Int) extends Json

case class DoubleValue(v: Double) extends Json

case class BooleanValue(v: Boolean) extends Json

case object NullValue extends Json

case class ObjectValue(v: Map[String, Json]) extends Json

case class ArrayValue(v: Seq[Json]) extends Json

class JsonParser(val input: ParserInput) extends Parser with StringBuilding {

  import Json._
  import org.parboiled2._
  import CharPredicate.{Digit, HexDigit}

  val WhiteSpaceChar: CharPredicate = CharPredicate(" \n\r\t\f")
  val QuoteBackslash: CharPredicate = CharPredicate("\"\\")
  val QuoteSlashBackSlash: CharPredicate = QuoteBackslash ++ "/"

  def sp(c: Char) = rule(zeroOrMore(WhiteSpaceChar) ~ c ~ zeroOrMore(WhiteSpaceChar))

  def Unicode = rule('u' ~ capture(
    HexDigit ~ HexDigit ~ HexDigit ~ HexDigit
  ) ~> (Integer.parseInt(_, 16)))


  def PlainChar = rule(!QuoteBackslash ~ ANY ~ appendSB())

  def Characters = rule(zeroOrMore(PlainChar | '\\' ~ EscapedChar))

  def EscapedChar =
    rule(
      QuoteSlashBackSlash ~ appendSB("\\\"")
        | 'b' ~ appendSB('\b')
        | 'f' ~ appendSB('\f')
        | 'n' ~ appendSB('\n')
        | 'r' ~ appendSB('\r')
        | 't' ~ appendSB('\t')
        | Unicode ~> { ch => sb.append(ch.asInstanceOf[Char]); () }
    )

  def String = rule {
    sp('"') ~ clearSB() ~ Characters ~ sp('"') ~ push(sb.toString)
  }

  def IntJson = rule {
    capture(optional('-') ~ oneOrMore(Digit)) ~> (_.toIntValue)
  }

  def NullJson = rule {
    capture("null") ~> (_.toNullValue)
  }

  def BoolJson = rule {
    capture("true" | "false") ~> (_.toBoolValue)
  }

  def DoubleJson = rule {
    capture(
      optional("-") ~ oneOrMore(Digit) ~ optional("." ~ zeroOrMore(Digit)) ~ optional('e' ~ optional("-") ~ oneOrMore(Digit))
    ) ~> (_.toDoubleValue)
  }

  def StringJson = rule {
    String ~> (_.toStringValue)
  }

  def ObjectJson: Rule1[ObjectValue] = rule {
    sp('{') ~ zeroOrMore(KeyValue).separatedBy(sp(',')) ~ sp('}') ~> (Json.toObject(_))
  }

  def KeyValue = rule {
    String ~ sp(':') ~ Value ~> ((_, _))
  }

  def Value = rule {
    DoubleJson | IntJson | NullJson | BoolJson | StringJson | ObjectJson | ArrayJson
  }

  def ArrayJson: Rule1[ArrayValue] = rule {
    sp('[') ~ zeroOrMore(Value).separatedBy(sp(',')) ~ sp(']') ~> (ArrayValue(_))
  }

  def FileJson = rule(zeroOrMore(WhiteSpaceChar) ~ Value ~ EOI)
}

object JsonParser {
  implicit def string2Parser(s: String): JsonParser = new JsonParser(s)
  def parse(input:String) = new JsonParser(input).FileJson.run() match {
    case Success(value) => value
    case Failure(exception) => throw exception
  }
}
