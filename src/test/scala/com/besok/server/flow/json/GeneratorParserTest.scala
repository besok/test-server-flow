package com.besok.server.flow.json

import org.scalatest.FunSuite
import Json._

object T extends ParserTester {
  override type P = GeneratorParser
  override type Result = GeneratorFunction
}

class GeneratorParserTest extends FunSuite {
  implicit var ctx: GeneratorContext = new GeneratorContextMap()
  test("sequence") {
    T.test(_.funcSequence.run()) {
      (" seq ( 1 ) ", (f: SequenceF) => assert(f.generate == 1.toJson))
    }
  }
  test("number") {
    T.test(_.funcNumber.run()) {
      ("num(1,100)", (f: NumberF) => f.generate match {
        case IntValue(v) => assert(v >= 1 && v <= 100)
      })
    }
  }

  test("str_from_file") {
    val expVal = Seq("a", "b", "c", "d", "e", "f")
    T.test(_.funcStrFromFile.run()) {
      ("str_file(C:\\projects\\test-server-flow\\target\\scala-2.13\\test-classes\\strings.txt)", (f: StringFromFileF) => f.generate match {
        case StringValue(v) => assert(expVal.contains(v))
        case _ => fail()
      })
      ("str_file(C:\\projects\\test-server-flow\\target\\scala-2.13\\test-classes\\strings_.txt,;)", (f: StringFromFileF) => f.generate match {
        case StringValue(v) => assert(expVal.contains(v))
        case _ => fail()
      })
    }
  }
  test("int_from_file") {
    val expVal = 1 to 7
    T.test(_.funcIntFromFile.run()) {
      ("num_file(C:\\projects\\test-server-flow\\target\\scala-2.13\\test-classes\\ints.txt,;)", (f: IntFromFileF) => f.generate match {
        case IntValue(v) => assert(expVal.contains(v))
        case _ => fail()
      })
    }
  }

  test("str_from_list") {
    val expList = Seq("a", "b", "c")
    T.test(_.funcStrFromList.run()) {
      ("str_list(a,b,c)", (f: FromListF[String]) => f.generate match {
        case StringValue(v) => assert(expList.contains(v))
        case _ => fail()
      })
    }
  }

  test("num_from_list") {
    val expList = 1 to 7
    T.test(_.funcNumFromList.run()) {
      ("num_list(1,2 ,3 ,4, 5  , 6 )", (f: FromListF[Int]) => f.generate match {
        case IntValue(v) => assert(expList.contains(v))
        case _ => fail()
      })
    }
  }

  test("uuid") {
    T.test(_.funcUUID.run()) {
      ("uuid()", (f: UuidF) => f.generate match {
        case StringValue(v) => assert(v.length == 36)
        case _ => fail()
      })
    }
  }
  test("time") {
    T.test(_.funcDate.run()) {
      ("time()", (f: DateF) => f.generate match {
        case StringValue(v) => assert(v.length == 36)
        case _ => fail()
      })
      ("time(Y-m-d H:M:S)", (f: DateF) => f.generate match {
        case StringValue(v) => assert(v.length >= 16)
        case _ => fail()
      })

    }
  }

  test("from_ctx") {
    ctx.put("t", StringValue("test"))
    T.test(_.funcCtx.run()) {
      ("from_ctx(t)", (f: FromContextF) => assert(f.generate == StringValue("test")))
    }
  }

  test("array") {
    T.test(_.funcArray.run()) {
      ("array(10,num(1,10))", (f: ArrayF) => f.generate match {
        case ArrayValue(v) => for (iv <- v) {
          iv match {
            case IntValue(i) => assert(i >= 0 && i <= 10)
            case _ => fail()
          }
        }
      })
    }
  }

  test("full_example") {
    T.test(_.functionWithCallback.run()) {
      ("uuid() => [var_name]", (f: GeneratorFunction) => f.generate match {
        case js@StringValue(v) =>
          assert(v.length == 36)
          assert(ctx.get("var_name") == js)
        case _ => fail()
      })
    }
  }


}
