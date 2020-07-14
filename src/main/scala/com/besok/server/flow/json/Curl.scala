package com.besok.server.flow.json
import scala.sys.process._

class Curl {
  def execute(curlCommand:String, json: String): String ={
    s"""curl $curlCommand -d"$json"""".!!
  }
}


