package com.besok.server.flow.json.parser

import org.scalatest.FunSuite

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try};

object T {

  implicit def string2Parser(s: String) = JsonParser(s)

  def test[T <: JsonValue](rule: => Try[T])(assertion: T => Unit): Unit = {
    rule match {
      case Success(value) => assertion(value)
      case Failure(exception) => throw exception
    }
  }
}

class JsonParserTest extends FunSuite {

  import T._

  test("basic") {
    T.test("123".Numbers.run())(p => assert(p == IntValue(123)))
  }
}
