package akka.sample.bikes

import akka.management.cluster.bootstrap.ClusterBootstrap
import com.typesafe.config.ConfigFactory

object MainK8 {
  def forMain(args: Array[String]): Unit = {
    val config = ConfigFactory.load("application.conf")
    val httpPort = config.getInt("bikes.httpPort")
    val system = Main.startNode(config, httpPort)

    ClusterBootstrap(system).start()
  }
}
