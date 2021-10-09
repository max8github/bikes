package akka.sample.bikes

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, SupervisorStrategy }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity }
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.{ ProjectionBehavior, ProjectionId }
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.sample.bikes.tree.GlobalTreeActor
import akka.{ actor => classic }
import com.typesafe.config.ConfigFactory

/**
 * See the README.md for starting each node with sbt.
 */
object Main {

  def main_kubernetes(args: Array[String]): Unit = {
    val config = ConfigFactory.load("application.conf")
    val httpHost = config.getString("bikes.httpHost")
    val httpPort = config.getInt("bikes.httpPort")

    //Create an Akka system and an actor that starts the sharding and receives messages.
    val rootBehavior = Behaviors.setup[Nothing] { context =>

      val globalTreeRef = ClusterSingleton(context.system).init(SingletonActor(Behaviors.supervise(
        GlobalTreeActor()).onFailure[Exception](SupervisorStrategy.restart), "GlobalTreeActor"))

      val numShards = context.system.settings.config.getInt("akka.cluster.sharding.number-of-shards")
      val messageExtractor = BikeMessageExtractor(numShards)

      val procurement = context.spawn(Procurement(context.system), "procurement")
      val shardingRegion = ClusterSharding(context.system).init(Entity(Bike.typeKey) { entityContext =>
        Bike(entityContext.entityId, BikeTags.Single, procurement, entityContext.shard, numShards)
      }.withStopMessage(GoodBye).withMessageExtractor(messageExtractor))

      val guardian = context.spawn(FleetsMaster(shardingRegion), "guardian")
      context.watch(guardian)

      context.spawn(ClusterListener(globalTreeRef), "ClusterListener")

      val routes = new BikeRoutes(guardian, globalTreeRef)(context.system).work ~ new WebSocketRoutes(globalTreeRef)(context).websocketRoute

      import akka.actor.typed.scaladsl.adapter._
      implicit val classicSystem: classic.ActorSystem = context.system.toClassic

      //#start-akka-management
      AkkaManagement.get(classicSystem).start()
      //#start-akka-management
      ClusterBootstrap.get(classicSystem).start()
      new BikeService(routes, httpHost, httpPort, context.system).start()

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "BikeService")
  }

  def main_local(args: Array[String]): Unit = {

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

    /**
     * Create an Akka system and an actor that starts the sharding
     * and receives messages.
     */
    def startNode(config: Config, httpPort: Int): Unit = {

      val rootBehavior = Behaviors.setup[Nothing] { context =>

        val globalTreeRef = ClusterSingleton(context.system).init(SingletonActor(Behaviors.supervise(
          GlobalTreeActor()).onFailure[Exception](SupervisorStrategy.restart), "GlobalTreeActor"))

        val numShards = context.system.settings.config.getInt("akka.cluster.sharding.number-of-shards")
        val messageExtractor = BikeMessageExtractor(numShards)

        val procurement = context.spawn(Procurement(context.system), "procurement")
        val shardingRegion = ClusterSharding(context.system).init(Entity(Bike.typeKey) { entityContext =>
          Bike(entityContext.entityId, BikeTags.Single, procurement, entityContext.shard, numShards)
        }.withStopMessage(GoodBye).withMessageExtractor(messageExtractor))

        val guardian = context.spawn(FleetsMaster(shardingRegion), "guardian")
        context.watch(guardian)

        val _ = context.spawn(ClusterListener(globalTreeRef), "ClusterListener")

        val routes = new BikeRoutes(guardian, globalTreeRef)(context.system).work ~ new WebSocketRoutes(globalTreeRef)(context).websocketRoute

        import akka.actor.typed.scaladsl.adapter._
        implicit val classicSystem: classic.ActorSystem = context.system.toClassic
        val host = config.getString("bikes.httpHost")
        new BikeService(routes, host, httpPort, context.system).start()

        //Projection Setup (createProjectionFor)
        val system = context.system
        implicit val ec = system.executionContext

        val sourceProvider: SourceProvider[Offset, EventEnvelope[Bike.Event]] =
          EventSourcedProvider.eventsByTag[Bike.Event](system, CassandraReadJournal.Identifier, BikeTags.Single)
        val projection = CassandraProjection.atLeastOnce(
          projectionId = ProjectionId("bikes", BikeTags.Single),
          sourceProvider,
          handler = () => new BikeEventsHandler(BikeTags.Single, system, globalTreeRef))

        context.spawn(ProjectionBehavior(projection), projection.projectionId.id)
        //Projection Setup ends

        Behaviors.empty
      }
      ActorSystem[Nothing](rootBehavior, "BikeService", config)
    }

    def config(port: Int): Config =
      ConfigFactory.parseString(
        s"""
       akka.remote.artery.canonical.port = $port
        """).withFallback(ConfigFactory.load("application_local.conf"))

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
      case _ =>
        // In a production application you wouldn't typically start multiple ActorSystem instances in the
        // same JVM. It is so here in order to easily demonstrate these ActorSystems (which would be in separate JVM's)
        // talking to each other. TODO: move to integ
        seedNodes ++ Seq(0)
    }

    val httpMyPort = ConfigFactory.load("application_local.conf").getInt("bikes.httpPort")
    val assignHttpPortTo = (akkaPort: Int) => if (akkaPort == 2553) httpMyPort else s"80${10 + random.nextInt(80)}".toInt

    for {
      akkaPort <- ports
      httpPort <- attemptHttpPort(assignHttpPortTo(akkaPort))
    } startNode(config(akkaPort), httpPort)
  }

  def main(args: Array[String]): Unit = if (runningLocally) main_local(args) else main_kubernetes(args)

  private def runningLocally = sys.env.getOrElse("RUN_LOCALLY", "true").toBoolean

  //  def runningInDocker(): Boolean = {
  //    //"com.jsuereth" %% "scala-arm" % "2.0"
  //    import resource._
  //    val filename = "/proc/self/cgroup"
  //    try {
  //      for (source <- managed(scala.io.Source.fromFile(filename))) {
  //        for (line <- source.getLines) {
  //          if (line.contains("docker")) {
  //            return true
  //          }
  //        }
  //      }
  //    } catch {
  //      case _: Exception => return false
  //    }
  //    false
  //  }
}
