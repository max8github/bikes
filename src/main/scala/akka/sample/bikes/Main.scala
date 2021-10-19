package akka.sample.bikes

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, SupervisorStrategy }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity }
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.http.scaladsl.server.Directives._
import akka.sample.bikes.tree.GlobalTreeActor
import akka.{ actor => classic }
import com.typesafe.config.Config

/**
 * See the README.md for starting each node with sbt.
 */
object Main {

  def startNode(config: Config): Unit = {

    val rootBehavior = Behaviors.setup[Nothing] { context =>

      val globalTreeRef = ClusterSingleton(context.system).init(SingletonActor(Behaviors.supervise(
        GlobalTreeActor()).onFailure[Exception](SupervisorStrategy.restart), "GlobalTreeActor"))

      val numShards = context.system.settings.config.getInt("akka.cluster.sharding.number-of-shards")
      val messageExtractor = BikeMessageExtractor(numShards)

      val procurement = context.spawn(Procurement(context.system), "procurement")
      val shardingRegion = ClusterSharding(context.system).init(Entity(Bike.typeKey) { entityContext =>
        val i = math.abs(entityContext.entityId.hashCode % BikeTags.Tags.size)
        val selectedTag = BikeTags.Tags(i)
        Bike(entityContext.entityId, selectedTag, procurement, entityContext.shard, numShards)
      }.withStopMessage(GoodBye).withMessageExtractor(messageExtractor))

      val guardian = context.spawn(FleetsMaster(shardingRegion), "guardian")
      context.watch(guardian)

      val _ = context.spawn(ClusterListener(globalTreeRef), "ClusterListener")

      val routes = new BikeRoutes(guardian, globalTreeRef)(context.system).work ~ new WebSocketRoutes(globalTreeRef)(context).websocketRoute

      import akka.actor.typed.scaladsl.adapter._
      implicit val classicSystem: classic.ActorSystem = context.system.toClassic
      val host = config.getString("bikes.httpHost")
      val httpPort = config.getInt("bikes.httpPort")
      new BikeService(routes, host, httpPort, context.system).start()

      BikeEventsProjection.init(context.system, globalTreeRef)

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "BikeService", config)
  }

  def main(args: Array[String]): Unit = if (runningLocally) MainLocal.main(args) else MainK8.main(args)

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
