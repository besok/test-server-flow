package com.besok.server.flow.json

import scopt.{OParser, OParserBuilder}

import scala.io.Source
import scala.util.Using

case class Config(var input: String = "", var port: Int = 9090)

object App {

  def main(args: Array[String]): Unit = {

    OParser.parse(parser, args, Config()) match {
      case Some(config) =>
        val yaml = Using(Source.fromFile(config.input))(_.mkString).get
        SystemConfigurer.setup(yaml, 8090)
      case _ =>
      // arguments are bad, error message will have been displayed
    }
  }

  val builder: OParserBuilder[Config] = OParser.builder[Config]
  val parser: OParser[Unit, Config] = {
    import builder._
    OParser.sequence(
      programName("test-server-flow"),
      head("the mock server for testing flows", "0.1"),
      opt[Int]('p', "port")
        .action((x, c) => {
          c.port = x
          c
        }).text("The port to kick off  the server"),
      opt[String]('c', "config")
        .action((x, c) => {
          c.input = x
          c
        })
        .required()
        .text("The file path to obtain the configuration parameters"),
    )
  }

}
