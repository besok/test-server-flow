package com.besok.server.flow.json.parser

import org.parboiled2.{Parser, ParserInput}

import scala.language.implicitConversions


abstract sealed class JsonValue

object JsonValue {

  implicit  class JsonValueHelper(v: String) {
    def toIntValue = IntValue(v.toInt)

    def string2DoubleValue = DoubleValue(v.toDouble)

    def string2BooleanValue = BooleanValue(v.toBoolean)

    def string2NullValue = NullValue
  }

}

case class StringValue(v: String) extends JsonValue

case class IntValue(v: Int) extends JsonValue

case class DoubleValue(v: Double) extends JsonValue

case class BooleanValue(v: Boolean) extends JsonValue

case object NullValue extends JsonValue

case class ObjectValue(v: Map[String, JsonValue]) extends JsonValue

case class ArrayValue(v: Seq[JsonValue]) extends JsonValue

case class JsonParser(input: ParserInput) extends Parser {

  import org.parboiled2._
  import JsonValue._

  def Numbers = rule {
    capture(optional('-') ~ oneOrMore(CharPredicate.Digit)) ~> (_.toIntValue)
  }
}
