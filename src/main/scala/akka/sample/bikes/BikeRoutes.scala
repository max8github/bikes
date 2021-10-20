package akka.sample.bikes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ ContentTypes, StatusCodes }
import akka.http.scaladsl.server.Route
import akka.sample.bikes.tree.{ GlobalTreeActor, Repr }
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

/**
 * HTTP API for
 * 1. Receiving data from remote work bikeIds
 *    A. Adding a new station when it comes online / goes offline
 *    B. Device samplings over windowed time slices
 * 2. Receiving and responding to queries
 *
 * @param guardian the entry point to the cluster and sharded data aggregates
 */
private[bikes] final class BikeRoutes(
  guardian: ActorRef[FleetsMaster.Command],
  globalTreeRef: ActorRef[GlobalTreeActor.TreeCommand])(implicit system: ActorSystem[_]) {

  implicit val timeout: Timeout = system.settings.config.getDuration("bikes.routes.ask-timeout").toMillis.millis

  import JsonSupport._
  import akka.http.scaladsl.server.Directives._

  val work: Route =
    pathPrefix("monitor") {
      getFromResource("monitor.html", ContentTypes.`text/html(UTF-8)`)
    } ~
      pathPrefix("bike") {
        concat(
          pathEnd {
            concat(
              get {
                val f: Future[BikeRoutesSupport.Inventory] = globalTreeRef.ask(replyTo => GlobalTreeActor.GetInventory(replyTo))
                onSuccess(f) { performed =>
                  complete(StatusCodes.OK -> performed)
                }
              },
              post {
                entity(as[Blueprint]) { blueprint =>
                  guardian ! FleetsMaster.WorkBike(blueprint)
                  complete(StatusCodes.Accepted -> s"${blueprint.makeEntityId()}")
                }
              })
          },
          path(Segment) { bikeId =>
            concat(
              get {
                val f: Future[BikeRoutesSupport.StatusResponse] = guardian.ask(replyTo => FleetsMaster.GetBike(bikeId, replyTo))
                onSuccess(f) { performed =>
                  complete(StatusCodes.OK -> performed)
                }
              },
              put {
                guardian.tell(FleetsMaster.KickBike(bikeId))
                complete(StatusCodes.Accepted -> s"$bikeId")
              },
              delete {
                guardian ! FleetsMaster.StopBike(bikeId)
                complete(StatusCodes.Accepted -> s"$bikeId")
              })
          })

      } ~
      pathPrefix("reserve") {
        path(Segment) { bikeId =>
          concat(
            put {
              guardian ! FleetsMaster.ReserveBike(bikeId)
              complete(StatusCodes.Accepted -> s"$bikeId")
            },
            delete {
              guardian ! FleetsMaster.YieldBike(bikeId)
              complete(StatusCodes.Accepted -> s"$bikeId")
            })
        }
      } ~
      pathPrefix("bikeid") {
        pathEnd {
          get {
            entity(as[Blueprint]) { blueprint =>
              complete(StatusCodes.OK -> s"${blueprint.makeEntityId()}")
            }
          }
        }
      } ~
      pathPrefix("tree") {
        pathEnd {
          get {
            import akka.sample.bikes.tree.Node
            val f: Future[Node] = globalTreeRef.ask(replyTo => GlobalTreeActor.GetJson(replyTo))
            onSuccess(f) { performed =>
              complete((StatusCodes.OK, performed))
            }
          }
        }
      }
}
