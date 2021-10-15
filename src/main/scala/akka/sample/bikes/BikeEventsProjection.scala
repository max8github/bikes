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

  def init(system: ActorSystem[Nothing], globalTreeRef: ActorRef[GlobalTreeActor.TreeCommand]) = {

    def sourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[Bike.Event]] =
      EventSourcedProvider.eventsByTag[Bike.Event](system, CassandraReadJournal.Identifier, tag)

    def projection(tag: String) =
      CassandraProjection.atLeastOnce(
        projectionId = ProjectionId("bikes", tag),
        sourceProvider(tag),
        handler = () => new BikeEventsHandler(tag, system, globalTreeRef))

    ShardedDaemonProcess(system).init[ProjectionBehavior.Command](
      name = "bikes",
      numberOfInstances = BikeTags.Tags.size,
      behaviorFactory = (i: Int) => ProjectionBehavior(projection(BikeTags.Tags(i))),
      stopMessage = ProjectionBehavior.Stop)
  }
}
