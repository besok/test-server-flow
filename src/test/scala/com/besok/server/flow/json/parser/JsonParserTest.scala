package com.besok.server.flow.json.parser

import org.scalatest.FunSuite

import scala.language.implicitConversions;

object JsonTester extends ParserTester {
  override type P = JsonParser
  override type Result = JsonValue

  implicit def string2Parser(s: String) = JsonParser(s)
}

class JsonParserTest extends FunSuite {

  import JsonTester._

  test("numbers") {
    JsonTester.test(_.Numbers.run()) {
      ("123", (p: IntValue) => assert(p == IntValue(123)))
      ("-10", (p: IntValue) => assert(p == IntValue(-10)))
      ("-0", (p: IntValue) => assert(p == IntValue(0)))
      ("0a", (p: IntValue) => assert(p == IntValue(0)))
    }
  }

  test("null"){
    JsonTester.test(_.Null.run()){
      ("null", p => assert(p == NullValue))
    }
  }
}
