enablePlugins(JavaAppPackaging)

name := "crypto-tool"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.12",
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "org.json4s" %% "json4s-native" % "3.6.0-M3",
  "com.github.scopt" %% "scopt" % "3.7.0",
  "de.vandermeer" % "asciitable" % "0.3.2")
