package akka.sample.bikes.tree

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.sample.bikes.tree

object GlobalTreeActor {

  trait TreeCommand
  final case class AddMember(memberId: String, event: String) extends TreeCommand
  final case class RemoveMember(memberId: String) extends TreeCommand
  final case class GetJson(replyTo: ActorRef[String]) extends TreeCommand

  def apply(): Behavior[TreeCommand] = {
    def updated(root: Node): Behavior[TreeCommand] = {
      import spray.json._
      import tree.NodeProtocol._
      Behaviors.receiveMessage[TreeCommand] {
        case AddMember(memberId, event) =>
          updated(tree.addOrUpdateMember(root, memberId, event))
        case RemoveMember(memberId) =>
          updated(tree.removeMember(root, memberId))
        case GetJson(replyTo) =>
          replyTo ! root.toJson.compactPrint
          Behaviors.same
      }
    }

    updated(Node("cluster", "cluster"))
  }
}
