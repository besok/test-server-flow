package com.besok.server.flow.json.parser

import org.parboiled2.{Parser, ParserInput, StringBuilding}

import scala.language.implicitConversions
import scala.util.{Failure, Success}


abstract sealed class Json {
  override def toString: String = this match {
    case NullValue => "null"
    case IntValue(v) => v.toString
    case DoubleValue(v) => v.toString
    case StringValue(v) => "\"" + v + "\""
    case BooleanValue(v) => v.toString
    case ArrayValue(arr) => s"""[${arr.mkString(",")}]"""
    case o@ObjectValue(_) => s"""{${Json.mapToString(o)}}"""
  }
}

object Json {

  implicit class PrettyJson[T<:Json](j: T) {
    def prettyString: String = formatStr(0)

    def formatStr(margin: Int): String = {
      this.j match {
        case ArrayValue(values) =>
          if (values.isEmpty) {
            "[]"
          } else {
            val sb = new StringBuilder
            sb ++= "["
            sb ++= values
              .map(_.formatStr(margin + 1))
              .fold("")(processInternalElems(margin))
            sb ++= s"\n${sp(margin - 1)}]"
            sb.toString
          }
        case ObjectValue(values) =>
          if (values.isEmpty) {
            "{}"
          } else {
            val sb = new StringBuilder
            sb ++= "{"
            sb ++= values
              .map{case (k:String,v:Json) => s"""${sp(margin+1)}"$k": ${v.formatStr(margin+2)}"""}
              .fold("")(processInternalElems(margin))
            sb ++= s"\n${sp(margin - 1)}}"
            sb.toString
          }
        case v => v.toString
      }
    }

    private def processInternalElems(margin: Int) = {
      (a: String, b: String) => {
        (a, b) match {
          case ("", b) => s"\n${sp(margin)}$b"
          case (a, "") => a
          case (a, b) => s"$a,\n${sp(margin)}$b"
        }
      }
    }

    def sp(i: Int): String = {
      if (i <= 0) "" else (for (_ <- 0 to i) yield {
        " "
      }).mkString
    }
  }

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

  def json: Json = FileJson.run() match {
    case Success(value) => value
    case Failure(exception) => throw exception
  }
}

object JsonParser {
  implicit def string2Parser(s: String): JsonParser = new JsonParser(s)
}
