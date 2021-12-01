package akka.sample.bikes

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, SupervisorStrategy }
import akka.cluster.typed.{ ClusterSingleton, SingletonActor }
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.sample.bikes.tree.GlobalTreeActor
import akka.{ actor => classic }
import com.typesafe.config.ConfigFactory

object MainKubernetes {
  def run(args: Array[String]): Unit = {
    val config = ConfigFactory.load("application.conf")

    val rootBehavior = Behaviors.setup[Nothing] { context =>

      val globalTreeRef = ClusterSingleton(context.system).init(SingletonActor(Behaviors.supervise(
        GlobalTreeActor()).onFailure[Exception](SupervisorStrategy.restart), "GlobalTreeActor"))

      val _ = context.spawn(ClusterListener(globalTreeRef), "ClusterListener")

      val routes = new BikeRoutes()(context.system).work ~ new WebSocketRoutes(globalTreeRef)(context).websocketRoute

      import akka.actor.typed.scaladsl.adapter._
      val classicSystem: classic.ActorSystem = context.system.toClassic

      //#start-akka-management
      AkkaManagement.get(classicSystem).start
      //#start-akka-management
      ClusterBootstrap.get(classicSystem).start()
      val host = config.getString("bikes.httpHost")
      val httpPort = config.getInt("bikes.httpPort")
      new BikeService(routes, host, httpPort)(context.system).start()

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "BikeService")
  }
}
