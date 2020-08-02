package com.besok.server.flow.json

object App {

  def main(args: Array[String]): Unit = {
    val yaml = scala.io.Source.fromFile("C:\\projects\\test-server-flow\\src\\test\\resources\\endpoints\\one_file.yml").mkString
    SystemConfigurer.setup(yaml,8090)
  }

}
