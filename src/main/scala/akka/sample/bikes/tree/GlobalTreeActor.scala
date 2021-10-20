package akka.sample.bikes.tree

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.sample.bikes.{ BikeRoutesSupport, CborSerializable, tree }

object GlobalTreeActor {

  trait TreeCommand extends CborSerializable
  final case class AddMember(memberId: String, event: String) extends TreeCommand
  final case class AddEntity(path: NodePath, state: String = "") extends TreeCommand
  final case class RemoveMember(memberId: String) extends TreeCommand
  final case class RemoveEntity(path: NodePath) extends TreeCommand
  final case class GetInventory(replyTo: ActorRef[BikeRoutesSupport.Inventory]) extends TreeCommand
  final case class GetJson(replyTo: ActorRef[Node]) extends TreeCommand
  case object Bye extends TreeCommand

  def apply(): Behavior[TreeCommand] = {
    def updated(root: Node): Behavior[TreeCommand] = {
      import spray.json._
      import tree.NodeProtocol._
      Behaviors.receiveMessage[TreeCommand] {
        case AddMember(memberId, event) =>
          updated(tree.addOrUpdateMember(root, memberId, event))
        case AddEntity(path, state) =>
          updated(tree.addOrUpdateEntity(root, path, state))
        case RemoveMember(memberId) =>
          updated(tree.removeMember(root, memberId))
        case RemoveEntity(path) =>
          updated(tree.removeEntity(root, path))
        case GetInventory(replyTo) =>
          val ids = tree.countLeaves(root)
          replyTo ! BikeRoutesSupport.Inventory(ids)
          Behaviors.same
        case GetJson(replyTo) =>
          replyTo ! root
          Behaviors.same
        case Bye =>
          // Possible async action then stop
          Behaviors.stopped
      }
    }

    updated(Node("cluster", "cluster"))
  }
}
