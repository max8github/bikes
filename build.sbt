import com.typesafe.sbt.packager.docker.DockerChmodType

ThisBuild /organization := "me.rerun"

//resolvers += Resolver.sonatypeRepo("snapshots")

name := """bikes"""

version := "0.3"

scalaVersion := "2.13.5"
lazy val akkaHttpVersion = "10.2.4"
lazy val akkaVersion    = "2.6.16"
lazy val akkaCassandraVersion    = "1.0.5"
lazy val scalatestVersion = "3.2.8"
val gatlingBundleName = "gatling-charts-highcharts-bundle"
val gatlingVersion = "3.5.1"

enablePlugins(GatlingPlugin)

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
Compile / run / fork := true

Compile / mainClass := Some("akka.sample.bikes.Main")

enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)

//See: https://www.scala-sbt.org/sbt-native-packager/formats/docker.html
//See: https://www.scala-sbt.org/sbt-native-packager/formats/universal.html#getting-started-with-universal-packaging
dockerExposedPorts := Seq(8084, 8558, 2553)
dockerEntrypoint := Seq("/opt/docker/bin/bikes", "2553")
dockerEnvVars := Map("RUN_LOCALLY" -> "false")
dockerAdditionalPermissions += (DockerChmodType.UserGroupWriteExecute, "/opt/docker/LMDB")//or DockerChmodType.Custom

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "org.scalatest" %% "scalatest-wordspec" % scalatestVersion % Test,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % Test,
    "io.gatling" % "gatling-test-framework" % gatlingVersion % Test,
    "io.gatling.highcharts" % gatlingBundleName % gatlingVersion artifacts (Artifact(gatlingBundleName, "zip", "zip", "bundle")) exclude("com.github.scopt", "scopt_2.10"),

    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,

    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,

    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,

    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.2",
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-multi-node-testkit"    % akkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % akkaVersion,

    //The addition of akka-persistence-cassandra brings all the following
    //transitive dependencies which have an older version than the one used for akka here causing
    //a dependency clash error. By explicitly adding thm all here, the error is fixed.
    "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaCassandraVersion,
    "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % akkaCassandraVersion % Test,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-coordination" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-protobuf-v3" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,//transitive to set
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,//transitive to set

    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.10",
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.10"
  )
}
