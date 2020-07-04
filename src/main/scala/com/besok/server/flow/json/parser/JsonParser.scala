package com.besok.server.flow.json.parser

import org.parboiled2.{Parser, ParserInput, StringBuilding}

import scala.language.implicitConversions


abstract sealed class JsonValue

object JsonValue {

  implicit class JsonValueHelper(v: String) {
    def toIntValue = IntValue(v.toInt)

    def toStringValue = StringValue(v)

    def toDoubleValue = DoubleValue(v.toDouble)

    def toBoolValue = BooleanValue(v.toBoolean)

    def toNullValue = NullValue
  }

}

case class StringValue(v: String) extends JsonValue

case class IntValue(v: Int) extends JsonValue

case class DoubleValue(v: Double) extends JsonValue

case class BooleanValue(v: Boolean) extends JsonValue

case object NullValue extends JsonValue

case class ObjectValue(v: Map[String, JsonValue]) extends JsonValue

case class ArrayValue(v: Seq[JsonValue]) extends JsonValue

case class JsonParser(input: ParserInput) extends Parser with StringBuilding {

  import JsonValue._
  import org.parboiled2._
  import CharPredicate.{Digit,HexDigit}

  val WhiteSpaceChar: CharPredicate = CharPredicate(" \n\r\t\f")
  val QuoteBackslash: CharPredicate = CharPredicate("\"\\")
  val QuoteSlashBackSlash: CharPredicate = QuoteBackslash ++ "/"

  def Ws(c: Char) = rule(c ~ rule(zeroOrMore(WhiteSpaceChar)))

  def Unicode = rule('u' ~ capture(
    HexDigit ~ HexDigit ~ HexDigit ~ HexDigit
  ) ~> (Integer.parseInt(_, 16)))


  def PlainChar = rule(!QuoteBackslash ~ ANY ~ appendSB())

  def Characters = rule(zeroOrMore(PlainChar | '\\' ~ EscapedChar))

  def EscapedChar =
    rule(
      QuoteSlashBackSlash ~ appendSB()
        | 'b' ~ appendSB('\b')
        | 'f' ~ appendSB('\f')
        | 'n' ~ appendSB('\n')
        | 'r' ~ appendSB('\r')
        | 't' ~ appendSB('\t')
        | Unicode ~> { ch => sb.append(ch.asInstanceOf[Char]); () }
    )


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
    '"' ~ clearSB() ~ Characters ~ Ws('"') ~ push(sb.toString) ~> (_.toStringValue)
  }

}
