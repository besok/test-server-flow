package com.besok.server.flow.json.parser

import org.scalatest.FunSuite;

object JsonParserTester extends JsonParser {
  def test[T <: JsonValue](parser: Parser[T])(args: (String, T => Unit)*): Unit = {
    for ((i, f) <- args) {
      parse(parser, i) match {
        case Success(matched, _) => f(matched)
        case Failure(msg, _) =>
          println(s"FAILURE: $msg");
          assert(false)
        case Error(msg, _) =>
          println(s"ERROR: $msg")
          assert(false)
      }
    }
  }
}

class JsonParserTest extends FunSuite {


  test("integer_basic") {
    JsonParserTester.test(JsonParserTester.integer) {
      ("123", (p: IntValue) => assert(p.v == 123))
      ("1", (p: IntValue) => assert(p.v == 1))
      ("0", (p: IntValue) => assert(p.v == 0))
      ("0123", (p: IntValue) => assert(p.v == 123))
    }
  }

}
