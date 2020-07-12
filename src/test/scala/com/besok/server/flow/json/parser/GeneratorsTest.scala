package com.besok.server.flow.json.parser

import java.text.SimpleDateFormat

import org.scalatest.FunSuite
import Json._
class GeneratorsTest extends FunSuite {

  test("seq") {
    val s = SequenceF(10)
    assert(s.generate == IntValue(10))
    assert(s.generate == IntValue(11))
    assert(s.generate == IntValue(12))
  }

  test("str") {
    val s = StringF(10)
    val v1 = s.generate
    val v2 = s.generate
    println(s"$v1 , $v2")

    assert(v1.toString.length == 12)
    assert(v2.toString.length == 12)
    assert(v1 != v2)
  }

  test("num") {
    val s = NumberF(-10, 0)
    val v1 = s.generate
    val v2 = s.generate

    println(s"$v1 , $v2")
    (v1, v2) match {
      case (IntValue(v1), IntValue(v2)) => {
        assert(v1 >= -10 && v1 <= 0)
        assert(v2 >= -10 && v2 <= 0)
        assert(v2 != v1)
      }
    }
  }
  test("from_file") {
    val cl = Thread.currentThread().getContextClassLoader

    val g1 = StringFromFileF(cl.getResource("strings.txt").getPath, ",")
    val expVal = Seq("a", "b", "c", "d", "e", "f")
    for (_ <- 0 to 10) {
      g1.generate match {
        case StringValue(v) => assert(expVal.contains(v))
      }
    }

    val g2 = IntFromFileF(cl.getResource("ints.txt").getPath, ";")
    val expInts = 1 to 7
    for (_ <- 0 to 10) {
      g2.generate match {
        case IntValue(v) => assert(expInts.contains(v))
      }
    }
  }

  test("from_List") {
    val s = Seq("a", "b", "c", "d")
    val fun = FromListF(s)
    for (_ <- 1 to 10) {
      assert(s.contains({
        fun.generate match {
          case StringValue(v) => v
        }
      }))
    }
  }

  test("uuid") {
    val f = UuidF()

    val v1 = f.generate
    val v2 = f.generate


    (v1, v2) match {
      case (StringValue(v1),StringValue(v2)) => {
        assert(v1 != v2)
        assert(v1.length == 36)
      }
    }
  }

  test("date"){
    val f = DateF(new SimpleDateFormat().toPattern)
    f.generate match {
      case StringValue(v) => assert(v.length == 15)
    }
  }

  test("array"){
    val inF = NumberF(1,100)
    val f = ArrayF(inF, 10)

    f.generate match {
      case ArrayValue(v) => assert(v.size == 10)
    }
  }

  test("from_ctx"){

    val f = FromContextF(new GeneratorContext {
      override def get(key: String): Json = 100.toJson

      override def put(key: String, value: Json): Option[Json] = ???
    },_.get(""))

    assert(f.generate == IntValue(100))
  }
}
