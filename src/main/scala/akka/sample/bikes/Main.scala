package akka.sample.bikes

import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity }
import akka.sample.bikes.tree.GlobalTreeActor

/**
 * See the README.md for starting each node with sbt.
 */
object Main {

  def main(args: Array[String]): Unit = {

    import java.net.{ DatagramSocket, InetSocketAddress }
    import java.nio.channels.DatagramChannel

    import akka.actor.AddressFromURIString
    import akka.actor.typed.scaladsl.Behaviors
    import akka.actor.typed.{ ActorSystem, SupervisorStrategy }
    import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
    import akka.http.scaladsl.server.Directives._
    import akka.{ actor => classic }
    import com.typesafe.config.{ Config, ConfigFactory }

    import scala.util.Random
    import scala.util.control.NonFatal

    val random = new Random()

    def startNode(config: Config, httpPort: Int): Unit = {

      val rootBehavior = Behaviors.setup[Nothing] { context =>

        val globalTreeRef = ClusterSingleton(context.system).init(SingletonActor(Behaviors.supervise(
          GlobalTreeActor()).onFailure[Exception](SupervisorStrategy.restart), "GlobalTreeActor"))

        val _ = context.spawn(ClusterListener(globalTreeRef), "ClusterListener")

        val routes = new BikeRoutes()(context.system).work ~ new WebSocketRoutes(globalTreeRef)(context).websocketRoute

        import akka.actor.typed.scaladsl.adapter._
        implicit val classicSystem: classic.ActorSystem = context.system.toClassic
        val host = config.getString("bikes.httpHost")
        new BikeService(routes, host, httpPort)(context.system).start()

        val shardingRegion = ClusterSharding(context.system).init(Entity(Bike.typeKey) { entityContext =>
          Bike(entityContext.entityId)
        })

        shardingRegion ! ShardingEnvelope("bikeId001", DownloadCmd("bikeId001"))

        Behaviors.empty
      }
      ActorSystem[Nothing](rootBehavior, "BikeService", config)
    }

    def config(port: Int): Config = ConfigFactory.parseString(s"""akka.remote.artery.canonical.port = $port""").
      withFallback(ConfigFactory.load("application_local.conf"))

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

    val seedNodes = akka.japi.Util
      .immutableSeq(ConfigFactory.load("application_local.conf").getStringList("akka.cluster.seed-nodes"))
      .flatMap { case AddressFromURIString(s) => s.port }

    val ports = args.headOption match {
      case Some(port) => Seq(port.toInt)
      case _ => seedNodes ++ Seq(0)
    }

    val httpMyPort = ConfigFactory.load("application_local.conf").getInt("bikes.httpPort")
    val assignHttpPortTo = (akkaPort: Int) => if (akkaPort == 2553) httpMyPort else s"80${10 + random.nextInt(80)}".toInt

    for {
      akkaPort <- ports
      httpPort <- attemptHttpPort(assignHttpPortTo(akkaPort))
    } startNode(config(akkaPort), httpPort)
  }
}
