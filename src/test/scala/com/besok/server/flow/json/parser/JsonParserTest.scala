package com.besok.server.flow.json.parser

import org.scalatest.FunSuite

import scala.language.implicitConversions;

object JsonTester extends ParserTester {
  override type P = JsonParser
  override type Result = JsonValue

  implicit def string2Parser(s: String) = JsonParser(s)
}

class JsonParserTest extends FunSuite {

  test("1"){
    print("!")
  }

  test("numbers") {
    JsonTester.test(_.IntJson.run()) {
      ("123", (p: IntValue) => assert(p == IntValue(123)))
      ("-10", (p: IntValue) => assert(p == IntValue(-10)))
      ("-0", (p: IntValue) => assert(p == IntValue(0)))
      ("0a", (p: IntValue) => assert(p == IntValue(0)))
    }
  }

  test("null") {
    JsonTester.test(_.NullJson.run()) {
      ("null", p => assert(p == NullValue))
    }
  }
  test("bool") {
    JsonTester.test(_.BoolJson.run()) {
      ("true", (p: BooleanValue) => assert(p == BooleanValue(true)))
      ("false", (p: BooleanValue) => assert(p == BooleanValue(false)))
    }
  }

  test("string") {
    JsonTester.test(_.StringJson.run()) {
      ("\"\"", (p: StringValue) => assert(p.v.equals("")))
      ("\"abc\"", (p: StringValue) => assert(p.v.equals("abc")))
      ("\"abc\\\"abc\"", (p: StringValue) => assert(p.v.equals("abc\"abc")))
      ("\"\\\"\"", (p: StringValue) => assert(p.v.equals("\"")))
    }
  }

  test("double") {
    JsonTester.test(_.DoubleJson.run()) {
      ("123.1", (p: DoubleValue) => assert(p.v == 123.1))
    }
  }
}
