name := "test-server-flow"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies += "org.parboiled" %% "parboiled" % "2.2.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.12"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.26"
libraryDependencies += "org.yaml" % "snakeyaml" % "1.26"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
