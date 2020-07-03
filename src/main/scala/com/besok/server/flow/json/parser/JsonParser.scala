package com.besok.server.flow.json.parser

import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers


abstract sealed class JsonValue

object JsonValue {
  implicit def string2IntValue(v: String) = IntValue(v.toInt)

  implicit def string2DoubleValue(v: String) = DoubleValue(v.toDouble)

  implicit def string2BooleanValue(v: String) = BooleanValue(v.toBoolean)

  implicit def string2NullValue(v: String) = NullValue
}

case class StringValue(v: String) extends JsonValue

case class IntValue(v: Int) extends JsonValue

case class DoubleValue(v: Double) extends JsonValue

case class BooleanValue(v: Boolean) extends JsonValue

case object NullValue extends JsonValue

case class ObjectValue(v: Map[String, JsonValue]) extends JsonValue

case class ArrayValue(v: Seq[JsonValue]) extends JsonValue

class JsonParser extends RegexParsers {
  override val whiteSpace: Regex = "[ \t\r\f]+".r

  override def skipWhitespace = true

  def integer = "([0-9]+)".r ^^ JsonValue.string2IntValue

}

