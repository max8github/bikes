package akka.sample.bikes

/**
 * See the README.md for starting each node with sbt.
 */
object Main {

  def main(args: Array[String]): Unit = {

    import akka.actor.AddressFromURIString
    import akka.actor.typed.ActorSystem
    import akka.actor.typed.scaladsl.Behaviors
    import com.typesafe.config.{ Config, ConfigFactory }

    def startNode(config: Config): Unit = {
      val rootBehavior = Behaviors.setup[Nothing] { _ => Behaviors.empty }
      ActorSystem[Nothing](rootBehavior, "BikeService", config)
    }

    def config(port: Int): Config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port = $port""").
      withFallback(ConfigFactory.load("application_local.conf"))

    val seedNodes = akka.japi.Util
      .immutableSeq(ConfigFactory.load("application_local.conf").getStringList("akka.cluster.seed-nodes"))
      .flatMap { case AddressFromURIString(s) => s.port }

    val ports = args.headOption match {
      case Some(port) => Seq(port.toInt)
      case _ => seedNodes ++ Seq(0)
    }

    ports.foreach { akkaPort => startNode(config(akkaPort)) }
  }
}
