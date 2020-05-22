organization in ThisBuild := "me.rerun"
name := """bikes"""
version := "0.1"

scalaVersion := "2.13.5"
lazy val akkaVersion = "2.6.16"

fork in run := true
Compile / run / fork := true
mainClass in (Compile, run) := Some("akka.sample.bikes.Main")

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}
