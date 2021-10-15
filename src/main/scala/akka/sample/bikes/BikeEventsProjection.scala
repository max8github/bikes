package akka.sample.bikes

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.{ ProjectionBehavior, ProjectionId }
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.sample.bikes.tree.GlobalTreeActor

object BikeEventsProjection {

  def init(context: ActorContext[_], globalTreeRef: ActorRef[GlobalTreeActor.TreeCommand]) = {
    val system = context.system
    implicit val ec = system.executionContext

    val sourceProvider: SourceProvider[Offset, EventEnvelope[Bike.Event]] =
      EventSourcedProvider.eventsByTag[Bike.Event](system, CassandraReadJournal.Identifier, BikeTags.Single)
    val projection = CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("bikes", BikeTags.Single),
      sourceProvider,
      handler = () => new BikeEventsHandler(BikeTags.Single, system, globalTreeRef))

    context.spawn(ProjectionBehavior(projection), projection.projectionId.id)
  }
}
