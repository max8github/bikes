package akka.sample.bikes

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent._
import akka.cluster.typed.{ Cluster, Subscribe }
import akka.actor.typed.scaladsl.LoggerOps
import akka.sample.bikes.tree.GlobalTreeActor

object ClusterListener {

  sealed trait Event extends CborSerializable
  // internal adapted cluster events only
  private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent) extends Event
  private final case class MemberChange(event: MemberEvent) extends Event

  def apply(treeActor: ActorRef[GlobalTreeActor.TreeCommand]): Behavior[Event] = Behaviors.setup { ctx =>
    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(MemberChange)
    Cluster(ctx.system).subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    val reachabilityAdapter = ctx.messageAdapter(ReachabilityChange)
    Cluster(ctx.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

    Behaviors.receiveMessage { message =>
      message match {
        case ReachabilityChange(reachabilityEvent) =>
          reachabilityEvent match {
            case UnreachableMember(member) =>
              ctx.log.info("Member detected as unreachable: {}", member)
            case ReachableMember(member) =>
              ctx.log.info("Member back to reachable: {}", member)
          }

        case MemberChange(changeEvent) =>
          changeEvent match {
            case MemberUp(member) =>
              ctx.log.info("MemberUp: {}", member.address)
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
            case MemberRemoved(member, previousStatus) =>
              treeActor ! GlobalTreeActor.RemoveMember(member.address.toString)
              ctx.log.info2("Member is Removed: {} after {}", member.address, previousStatus)
            case MemberJoined(member) =>
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
              ctx.log.info("Member is Joined: {}", member.address)
            case MemberWeaklyUp(member) =>
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
              ctx.log.info("Member is weakly up: {}", member.address)
            case MemberLeft(member) =>
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
              ctx.log.info("Member {} left", member.address)
            case MemberExited(member) =>
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
              ctx.log.info("Member {} exited", member.address)
            case MemberDowned(member) =>
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
              ctx.log.info("Member {} downed", member.address)
            case MemberPreparingForShutdown(member) =>
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
              ctx.log.info("Member {} preparing for shutdown", member.address)
            case MemberReadyForShutdown(member) =>
              treeActor ! GlobalTreeActor.AddMember(member.address.toString, changeEvent.getClass.getSimpleName)
              ctx.log.info("Member {} ready for shutdown", member.address)
          }
      }
      Behaviors.same
    }
  }
}