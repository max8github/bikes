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

object BikeRoutes extends SprayJsonSupport {
  sealed trait Status
  final case class BikeRemoved(bikeId: String) extends Status
  final case class Inventory(entities: List[Repr]) extends Status
  final case class QueryStatus(bikeId: String, state: Bike.State) extends Status
  private final case class WrappedResponse(response: Bike.State) extends Status
}

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
                val f: Future[BikeRoutes.Inventory] = globalTreeRef.ask(replyTo => GlobalTreeActor.GetInventory(replyTo))
                onSuccess(f) { performed =>
                  complete(StatusCodes.OK -> performed)
                }
              },
              post {
                entity(as[Bike.Blueprint]) { blueprint =>
                  guardian ! FleetsMaster.WorkBike(blueprint)
                  complete(StatusCodes.Accepted -> s"${blueprint.makeEntityId()}")
                }
              })
          },
          path(Segment) { bikeId =>
            concat(
              get {
                val f: Future[BikeRoutes.QueryStatus] = guardian.ask(replyTo => FleetsMaster.GetBike(bikeId, replyTo))
                onComplete(f) {
                  case Success(performed) =>
                    val st = performed.state.getClass.getSimpleName
                    val resp =
                      if (st.endsWith("$")) st.replace("$", "")
                      else if (st.startsWith("Error")) performed.state.toString
                      else if (st.contains("Reserved")) performed.state.toString
                      else st
                    complete(StatusCodes.OK -> resp)
                  case Failure(_) => complete(StatusCodes.NotFound)
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
            entity(as[Bike.Blueprint]) { blueprint =>
              complete(StatusCodes.OK -> s"${blueprint.makeEntityId()}")
            }
          }
        }
      }
}
