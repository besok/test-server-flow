package com.besok.server.flow.json.parser

import org.parboiled2.{Parser, ParserInput}

class GeneratorParser(val input: ParserInput) extends Parser {

  import org.parboiled2._
  import CharPredicate.Digit

  val WhiteSpaceChar: CharPredicate = CharPredicate(" \n\r\t\f")

  def sp(c: Char) = rule(zeroOrMore(WhiteSpaceChar) ~ c ~ zeroOrMore(WhiteSpaceChar))

  def funcName(funcName: String) = rule {
    zeroOrMore(WhiteSpaceChar) ~ funcName ~ sp('(')
  }

  def posNum = rule {
    capture(oneOrMore(Digit)) ~> (_.toInt)
  }


  def funcSequence = rule {
    funcName("seq") ~ posNum ~ sp(')') ~> SequenceF
  }

  def funcString = rule {
    funcName("str") ~ posNum ~ sp(')') ~> StringF
  }

  def funcNumber = rule {
    funcName("num") ~ posNum ~ sp(',') ~ posNum ~ sp(')') ~> NumberF
  }

  def funcStrFromFile = rule {
    funcName("str_file") ~ parsefromFile ~>
      ((p: String, s: Option[String]) => StringFromFileF(p, s.map(_.charAt(0)).getOrElse(',')))
  }

  def funcIntFromFile = rule {
    funcName("num_file") ~ parsefromFile ~>
      ((p: String, s: Option[String]) => IntFromFileF(p, s.map(_.charAt(0)).getOrElse(',')))
  }


  def funcStrFromList = rule {
    funcName("str_list") ~ capture(zeroOrMore(STR).separatedBy(sp(','))) ~ sp(')') ~> (v => FromListF(v))
  }

  def parsefromFile = rule {
    capture(internalString) ~ optional(sp(',') ~ capture(internalString)) ~ sp(')')
  }

  def internalString = rule {
    oneOrMore(STR)
  }

  val STR = CharPredicate.All -- ',' -- ')'
}

object GeneratorParser {
  implicit def stringToParser(input: String) = new GeneratorParser(input)
}
