package com.besok.server.flow.json

import org.scalatest.FunSuite

class SenderTest extends FunSuite {

  test("curl"){
    val json = ObjectValue(Map(
      "a" -> StringValue("a")
    )).toPrettyString

    println(
      new Curl().execute("-X POST 127.0.0.1:7878 -H Content-Type:application/json", json)
    )

  }

}
