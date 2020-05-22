package akka.sample.bikes

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.Done

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

/**
 * Bike HTTP Server.
 * @param routes
 * @param host
 * @param port
 * @param system
 */
private[bikes] final class BikeService(routes: Route, host: String, port: Int)(implicit system: ActorSystem[_]) {

  private val shutdown = CoordinatedShutdown(system)

  implicit val m = Materializer(system)
  implicit val untyped = system.classicSystem
  implicit val dispatcher = untyped.dispatcher

  def start(): Unit = {
    Http()(system.classicSystem).bindAndHandle(routes, host, port).onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("BikeService online at http://{}:{}/", address.getHostString, address.getPort)

        shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-graceful-terminate") { () =>
          binding.terminate(10.seconds).map { _ =>
            system.log.info("BikeService http://{}:{}/ graceful shutdown completed", address.getHostString, address.getPort)
            Done
          }
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
