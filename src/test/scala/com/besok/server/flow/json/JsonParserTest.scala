package com.besok.server.flow.json

import org.scalatest.FunSuite

import scala.language.implicitConversions;

object JsonTester extends ParserTester {
  override type P = JsonParser
  override type Result = Json
}

class JsonParserTest extends FunSuite {

  import JsonParser._

  test("numbers") {
    JsonTester.test(_.NumberJson.run()) {
      ("123", (p: Json) => assert(p == IntValue(123)))
      ("-10", (p: Json) => assert(p == IntValue(-10)))
      ("-0", (p: Json) => assert(p == IntValue(0)))
      ("0a", (p: Json) => assert(p == IntValue(0)))
      ("123.1", (p: Json) => assert(p == DoubleValue(123.1)))
      ("-123", (p: Json) => assert(p == DoubleValue(-123)))
      ("1e10", (p: Json) => assert(p == DoubleValue("1e10".toDouble)))
      ("1e-10", (p: Json) => assert(p == DoubleValue("1e-10".toDouble)))
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
      ("\"abc\\\"abc\"", (p: StringValue) => assert(p.v.equals("abc\\\"abc")))
      ("\"\\\"\"", (p: StringValue) => assert(p.v.equals("\"")))
      ("\"string with \\\" in the middle \\\" of the \\\" string\"", (p: StringValue) => {
        assert(p.v.equals("string with \\\" in the middle \\\" of the \\\" string"))
      })
      (""" "a\"a" """, (p: StringValue) => {
        assert(p.v.equals("""a\"a"""))
      })
    }
  }

  test("object") {
    JsonTester.test(_.ObjectJson.run()) {
      ("""{ "a" : 123 }""", (p: Json) => assert(p == ObjectValue(Map("a" -> IntValue(123)))))
      ("""{ "a" : 123.1 }""", (p: Json) => assert(p == ObjectValue(Map("a" -> DoubleValue(123.1)))))
      ("""{ "a" : "string" }""", (p: Json) => assert(p == ObjectValue(Map("a" -> StringValue("string")))))
      (
        """{
          "a" : "string" ,
          "b" : { "c" :"d" }
          }""", (p: Json) => assert(
        p == ObjectValue(
          Map(
            "a" -> StringValue("string"),
            "b" -> ObjectValue(Map("c" -> StringValue("d")))
          )
        )
      ))
    }
  }

  test("array") {
    JsonTester.test(_.ArrayJson.run()) {
      ("""[]""", (p: Json) => assert(p == ArrayValue(Seq.empty)))
      ("""[null]""", (p: Json) => assert(p == ArrayValue(Seq(NullValue))))
      ("""[true,false]""", (p: Json) => assert(p == ArrayValue(Seq(BooleanValue(true), BooleanValue(false)))))
      ("""[{},{}]""", (p: Json) => assert(p == ArrayValue(Seq(ObjectValue(Map.empty), ObjectValue(Map.empty)))))
    }
  }

  test("to_string") {
    assert("null".equals(NullValue.toString))
    assert("123".equals(IntValue(123).toString))
    assert(123.1e1.toString.equals(DoubleValue(123.1e1).toString))
    assert("\"abc\"".equals(StringValue("abc").toString))
    assert("true".equals(BooleanValue(true).toString))
    assert("[1,\"abc\",null]".equals(ArrayValue(Seq(IntValue(1), StringValue("abc"), NullValue)).toString))
    assert(
      """{"a": [1],"b": "abc"}""".equals(ObjectValue(Map(
        "a" -> ArrayValue(Seq(IntValue(1))),
        "b" -> StringValue("abc")
      )).toString))

    val value = StringValue("string with \\\" in the middle \\\" of the \\\" string")
    assert(
      """"string with \" in the middle \" of the \" string"""".equals(
        value.toString
      ))
  }

  test("to_pretty_string") {
    import Json._

    assert(ArrayValue(Seq()).toPrettyString.equals("[]"))

    var prStr = ArrayValue(Seq(IntValue(1), IntValue(2), IntValue(3))).formatStr(10)
    assert(prStr.equals("[\n           1,\n           2,\n           3\n          ]"))

    prStr = ObjectValue(Map(
      "a" -> ArrayValue(Seq(StringValue("ab"), StringValue("abc"))),
      "b" -> ObjectValue(Map("b" -> ArrayValue(Seq(BooleanValue(true))), "c" -> NullValue))
    )).toPrettyString
    assert(prStr.equals("{\n  \"a\": [\n   \"ab\",\n   \"abc\"\n  ],\n  \"b\": {\n       \"b\": [\n     true\n    ],\n       \"c\": null\n  }\n}"))

  }

  test("simple") {
    assert("""{"a": "abc\"abc"}""".intoJson.get == ObjectValue(Map("a" -> StringValue("abc\\\"abc"))))
  }

  test("number") {
    JsonTester.test(_.NumberJson.run()) {
      ("123.321", (p: Json) => assert(p == DoubleValue(123.321)))
    }
  }

  test("complex") {
    import scala.io.Source
    val txt = Source.fromResource("jsons/example_standart.json").mkString

    val json = txt.intoJson
    println(json)
  }

  test("query") {
    import scala.io.Source
    val txt = Source.fromResource("jsons/example_standart.json").mkString

    val json = txt.intoJson.get

    json.query("complex.objects.simple.id") match {
      case None => fail()
      case Some(v) => assert(v == IntValue(1))
    }

  }
}
