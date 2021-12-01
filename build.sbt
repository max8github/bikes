organization in ThisBuild := "me.rerun"
name := """bikes"""
version := "0.2.2"

scalaVersion := "2.13.5"
lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion = "2.6.16"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
fork in run := true
Compile / run / fork := true
mainClass in (Compile, run) := Some("akka.sample.bikes.Main")

enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)

//See: https://www.scala-sbt.org/sbt-native-packager/formats/docker.html
//See: https://www.scala-sbt.org/sbt-native-packager/formats/universal.html#getting-started-with-universal-packaging
dockerExposedPorts := Seq(8084, 8558, 2553)
dockerEntrypoint := Seq("/opt/docker/bin/bikes", "2553")
dockerEnvVars := Map("RUN_LOCALLY" -> "false")

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
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,

    "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % akkaVersion,

    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.5",
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.5"
  )
}
