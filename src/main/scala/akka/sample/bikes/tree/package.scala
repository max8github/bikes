package akka.sample.bikes

import org.slf4j.LoggerFactory
import spray.json.{ DefaultJsonProtocol, JsonFormat }

package object tree {

  private val log = LoggerFactory.getLogger("bikes.tree")

  case class Node(name: String, `type`: String, var nodeState: String = "", var children: Set[Node] = Set.empty) {
    override def equals(other: Any): Boolean = other match {
      case that: Node =>
        (that canEqual this) &&
          name == that.name &&
          `type` == that.`type`
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(name, `type`)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }

    override def toString: String = s"($name, $nodeState)"

    def representation(): Repr = Repr(name, nodeState)
  }
  case class Repr(id: String, state: String)
  case class NodePath(memberId: String, shardId: String, entityId: String) {
    override def toString: String = s"$memberId/$shardId/$entityId"
  }

  object NodeProtocol {
    import DefaultJsonProtocol._
    implicit val nodeFormat: JsonFormat[Node] = lazyFormat(jsonFormat(Node, "name", "type", "nodeState", "children"))
    implicit val nodeReprFormat = jsonFormat2(Repr)
  }

  private def findMemberFromRoot(root: Node, memberId: String): Option[Node] = root.children.find(_.name == memberId)

  def addOrUpdateMember(root: Node, memberId: String, state: String): Node = {
    findMemberFromRoot(root, memberId) match {
      case None =>
        val newMember = Node(memberId, "member", state)
        root.children += newMember
        root
      case Some(node) =>
        node.nodeState = state
        root
    }
  }

  def removeMember(root: Node, memberId: String): Node = {
    root.children -= Node(memberId, "member")
    root
  }
}

