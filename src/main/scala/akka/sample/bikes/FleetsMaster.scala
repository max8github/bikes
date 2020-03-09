package akka.sample.bikes

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity }
import akka.sample.bikes.Procurement.Operation
import akka.sample.bikes.tree.GlobalTreeActor

/** The entry point to the cluster and sharded data. Guardian of bikes. */
private[bikes] object FleetsMaster {

  type BikeId = String

  sealed trait Command
  /**
   * Data received by the cluster from externally.
   */
  final case class WorkBike(blueprint: Bike.Blueprint) extends Command
  final case class KickBike(bikeId: BikeId) extends Command
  final case class ReserveBike(bikeId: BikeId) extends Command
  final case class YieldBike(bikeId: BikeId) extends Command
  final case class GetBike(bikeId: BikeId, replyTo: ActorRef[BikeRoutes.QueryStatus]) extends Command
  final case class StopBike(bikeId: BikeId) extends Command

  def apply(actorTreeRef: ActorRef[GlobalTreeActor.TreeCommand]): Behavior[FleetsMaster.Command] =
    Behaviors.setup { context =>

      val numShards = context.system.settings.config.getInt("akka.cluster.sharding.number-of-shards")
      val messageExtractor = BikeMessageExtractor(numShards)

      def init(system: ActorSystem[_], ops: ActorRef[Operation], actorTreeRef: ActorRef[GlobalTreeActor.TreeCommand]) =
        ClusterSharding(system).init(Entity(Bike.typeKey) { entityContext =>
          Bike(entityContext.entityId, ops, actorTreeRef, entityContext.shard, numShards)
        }.withStopMessage(Bike.GoodBye).withMessageExtractor(messageExtractor))
      val ops = context.spawn(Procurement(context.system), "procurement")
      val shardingRegion = init(context.system, ops, actorTreeRef)
      new FleetsMaster(shardingRegion, actorTreeRef).active()
    }
}

private[bikes] final class FleetsMaster(
  shardingRegion: ActorRef[ShardingEnvelope[Bike.Command]],
  actorTreeRef: ActorRef[GlobalTreeActor.TreeCommand]) {

  def active(): Behavior[FleetsMaster.Command] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case FleetsMaster.WorkBike(blueprint) =>
          context.log.debug("FleetsMaster received a POST Create Bike request for bike {}", blueprint.displayId)
          shardingRegion ! ShardingEnvelope(blueprint.makeEntityId(), Bike.DownloadCmd(blueprint))
          Behaviors.same

        case FleetsMaster.ReserveBike(bikeId) =>
          context.log.debug("FleetsMaster received a request for reserving bike {}", Bike.displayOfId(bikeId))
          shardingRegion ! ShardingEnvelope(bikeId, Bike.ReserveCmd)
          Behaviors.same

        case FleetsMaster.YieldBike(bikeId) =>
          context.log.debug("FleetsMaster received a request for yielding bike {}", Bike.displayOfId(bikeId))
          shardingRegion ! ShardingEnvelope(bikeId, Bike.YieldCmd)
          Behaviors.same

        case FleetsMaster.KickBike(bikeId) =>
          context.log.debug("FleetsMaster received a PUT Kick request for bike {}", Bike.displayOfId(bikeId))
          shardingRegion ! ShardingEnvelope(bikeId, Bike.KickCmd)
          Behaviors.same

        case FleetsMaster.GetBike(bikeId, replyTo) =>
          context.log.debug("FleetsMaster received a GET request for bike {}", bikeId)
          shardingRegion ! ShardingEnvelope(bikeId, Bike.GetStateCmd(bikeId, replyTo))
          Behaviors.same

        case FleetsMaster.StopBike(bikeId) =>
          context.log.debug("FleetsMaster received a STOP request for bike {}", bikeId)
          shardingRegion ! ShardingEnvelope(bikeId, Bike.Idle)
          Behaviors.same

        case _ => Behaviors.unhandled
      }
    }
  }
}

