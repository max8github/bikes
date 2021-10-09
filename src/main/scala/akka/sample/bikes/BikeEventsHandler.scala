package akka.sample.bikes

import akka.Done
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.projection.eventsourced.EventEnvelope

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.projection.scaladsl.Handler
import akka.sample.bikes.tree.{ GlobalTreeActor }

class BikeEventsHandler(tag: String, system: ActorSystem[_], globalTreeRef: ActorRef[GlobalTreeActor.TreeCommand])
  extends Handler[EventEnvelope[Bike.Event]]() {
  import Bike._

  private implicit val ec: ExecutionContext = system.executionContext
  private implicit val numShards: Int = system.settings.config.getInt("akka.cluster.sharding.number-of-shards")

  override def process(envelope: EventEnvelope[Event]): Future[Done] = {
    envelope.event match {
      case DownloadEvent(blueprint) =>
        system.log.info("******************************* DownloadEvent")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "DownloadingState")
        Future.successful(Done)
      case DownloadedEvt(blueprint) =>
        system.log.info("******************************* DownloadedEvt")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "DownloadedState")
        Future.successful(Done)
      case CreateEvent(blueprint) =>
        system.log.info("******************************* CreateEvent")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "CreatingState")
        Future.successful(Done)
      case CreatedEvt(blueprint, location) =>
        system.log.info("******************************* CreatedEvt")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "CreatedState")
        Future.successful(Done)
      case ReserveEvent(blueprint) =>
        system.log.info("******************************* ReserveEvent")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "ReservingState")
        Future.successful(Done)
      case ReservedEvt(blueprint) =>
        system.log.info("******************************* ReservedEvt")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "ReservedState")
        Future.successful(Done)
      case YieldEvent(blueprint) =>
        system.log.info("******************************* YieldEvent")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "YieldingState")
        Future.successful(Done)
      case YieldedEvt(blueprint) =>
        system.log.info("******************************* YieldedEvt")
        val path = fullPath(blueprint.makeEntityId(), system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, "YieldedState")
        Future.successful(Done)
      case evt =>
        system.log.info(s"Unhandled event: $evt")
        Future.successful(Done)
    }
  }
}