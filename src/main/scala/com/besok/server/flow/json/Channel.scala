package com.besok.server.flow.json

import akka.http.scaladsl.model.HttpRequest

import scala.sys.process._

class Curl {
  def execute(curlCommand: String, json: String): String = {
    s"""curl $curlCommand -d"$json"""".!!
  }
}

class Request {
  def send = {
    HttpRequest()
  }
}
