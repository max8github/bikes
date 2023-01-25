package akka.sample.bikes

import akka.actor.AddressFromURIString
import com.typesafe.config.{ Config, ConfigFactory }

import java.net.{ DatagramSocket, InetSocketAddress }
import java.nio.channels.DatagramChannel
import scala.util.Random
import scala.util.control.NonFatal

object MainLocal {
  def forMain(args: Array[String]): Unit = {
    val config = ConfigFactory.load("application_local.conf")
    val httpPort = config.getInt("bikes.httpPort")
    val mngPort = config.getInt("akka.management.http.port")

    for {
      akkaPort <- calculatePorts(args, config)
      port <- attemptHttpPort(if (akkaPort == 2553) httpPort else s"80${10 + Random.nextInt(80)}".toInt)
      managementPort <- attemptHttpPort(if (akkaPort == 2553) mngPort else s"85${59 + Random.nextInt(41)}".toInt)
    } Main.startNode(configOverride(akkaPort, managementPort, config), port)
  }

  def attemptHttpPort(attempt: Int): Option[Int] = {
    val ds: DatagramSocket = DatagramChannel.open().socket()
    try {
      ds.bind(new InetSocketAddress("localhost", attempt))
      Some(attempt)
    } catch {
      case NonFatal(e) =>
        ds.close()
        println(s"Unable to bind to port $attempt for http server to send data: ${e.getMessage}")
        None
    } finally
      ds.close()
  }

  def configOverride(port: Int, mngPort: Int, config: Config): Config =
    ConfigFactory.parseString(
      s"""
       akka.remote.artery.canonical.port = $port
       akka.management.http.port = $mngPort
       akka.management.http.bind-port = $mngPort
        """).withFallback(config)

  def calculatePorts(args: Array[String], config: Config) = {
    val seedNodes = akka.japi.Util
      .immutableSeq(config.getStringList("akka.cluster.seed-nodes"))
      .flatMap { case AddressFromURIString(s) => s.port }

    args.headOption match {
      case Some(port) => Seq(port.toInt)
      case _ =>
        // In a production application you wouldn't typically start multiple ActorSystem instances in the
        // same JVM. It is so here in order to easily demonstrate these ActorSystems (which would be in separate JVM's)
        // talking to each other. TODO: move to integ
        seedNodes ++ Seq(0)
    }
  }
}
