name := "picture-voter"
organization := "com.wawok"
version := "1.0"
scalaVersion := "2.11.7"

scalacOptions in Global ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint")

mainClass in assembly := Some("com.wawok.PictureVoterService")

assemblyJarName in assembly := "PictureVoterService.jar"



val AKKA_VERSION = "2.0-M1"

libraryDependencies ++= Seq(
  //akka!
  "com.typesafe.akka" %% "akka-http-experimental" % AKKA_VERSION,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % AKKA_VERSION,

  //database!
  "com.h2database" % "h2" % "1.4.190",
  "com.typesafe.slick" %% "slick" % "3.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.0.13",

  //tests!
   "com.typesafe.akka" %% "akka-http-testkit-experimental" % AKKA_VERSION % "test",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)
