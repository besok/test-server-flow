package com.besok.server.flow.json.parser

import org.scalatest.FunSuite
import Json._
object GenParserTester extends ParserTester {
  override type P = GeneratorParser
  override type Result = GeneratorFunction
}

class GeneratorParserTest extends FunSuite {
  test("sequence") {
    GenParserTester.test(_.funcSequence.run()) {
      (" seq ( 1 ) ", (f: SequenceF) => assert(f.generate == 1.toJson))
    }
  }
  test("number"){
    GenParserTester.test(_.funcNumber.run()){
      ("num(1,100)", (f:NumberF) => f.generate match {
        case IntValue(v) => assert( v >= 1 && v <= 100)
      })
    }
  }

  test("str_from_file"){
    val expVal = Seq("a", "b", "c", "d", "e", "f")
    GenParserTester.test(_.funcStrFromFile.run()){
      ("str_file(C:\\projects\\test-server-flow\\target\\scala-2.13\\test-classes\\strings.txt)",(f:StringFromFileF) => f.generate match {
        case StringValue(v) => assert(expVal.contains(v))
      })
    }
  }


}
