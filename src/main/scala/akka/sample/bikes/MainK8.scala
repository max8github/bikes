package akka.sample.bikes

import com.typesafe.config.ConfigFactory

object MainK8 {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("application.conf")
    val httpPort = config.getInt("bikes.httpPort")
    Main.startNode(config, httpPort)
  }
}
