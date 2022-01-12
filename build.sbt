ThisBuild / organization := "me.rerun"
name := """bikes"""
version := "0.2"

scalaVersion := "2.13.5"
lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion = "2.6.16"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
Compile / run / fork := true
Compile / mainClass := Some("akka.sample.bikes.Main")


libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,

    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,

    "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % akkaVersion,

    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.2",
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,

    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.5",
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.5"
  )
}
