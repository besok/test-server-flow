package com.besok.server.flow.json

import org.parboiled2.Parser

import scala.util.{Failure, Success, Try}

trait ParserTester {
  type P <: Parser
  type Result

  def test[R <: Result](rule: P => Try[R])(ps: (P, R => Unit)*): Unit = {
    for ((p, a) <- ps) {
      rule(p) match {
        case Success(value) => a(value)
        case Failure(exception) =>
          throw exception
      }
    }
  }
}
