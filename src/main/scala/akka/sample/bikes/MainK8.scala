package akka.sample.bikes

import com.typesafe.config.ConfigFactory

object MainK8 {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("application.conf")
    Main.startNode(config)
  }
}
