package akka.sample.bikes

import akka.actor.typed.scaladsl.LoggerOps
import org.slf4j.LoggerFactory
import spray.json.{ DefaultJsonProtocol, JsonFormat }

package object tree {

  private val log = LoggerFactory.getLogger("bikes.tree")

  case class Node(name: String, `type`: String, var nodeState: String = "", var children: Set[Node] = Set.empty) {
    override def equals(other: Any): Boolean = other match {
      case that: Node =>
        (that canEqual this) && name == that.name && `type` == that.`type`
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

  private def findShardFromRoot(root: Node, shardId: String): Option[Node] =
    (for {
      member <- root.children
      shard <- member.children
    } yield shard).find(_.name == shardId)

  def findOrCreateShardWithPath(root: Node, memberId: String, shardId: String): Option[Node] = {
    val member = findMemberFromRoot(root, memberId)
    member match {
      case None => None
      case Some(m) =>
        val shard = m.children.find(_.name == shardId)
        shard match {
          case sh: Some[Node] => sh
          case None =>
            val newNode = Node(shardId, "shard")
            m.children += newNode
            Some(newNode)
        }
    }
  }

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

  def addShardFromRoot(root: Node, memberId: String, shardId: String): Node = {
    findMemberFromRoot(root, memberId) match {
      case None =>
        log.debug("ERROR: member {} not found while calling addShardFromRoot(), returning root", memberId)
        root
      case Some(n) =>
        val newNode = Node(shardId, "shard")
        n.children += newNode
        root
    }
  }

  def addOrUpdateEntity(root: Node, path: NodePath, state: String = ""): Node = {
    findOrCreateShardWithPath(root, path.memberId, path.shardId) match {
      case None =>
        log.debug("no member found when calling addOrUpdateEntity() on path {}, returning root", path)
        root
      case Some(shard) =>
        val newNode = Node(path.entityId, "entity", state)
        shard.children -= newNode
        shard.children += newNode
        root
    }
  }

  def removeMember(root: Node, memberId: String): Node = {
    root.children -= Node(memberId, "member")
    root
  }

  def removeShardFromRoot(root: Node, memberId: String, shardId: String): Node = {
    findMemberFromRoot(root, memberId) match {
      case None =>
        log.debug("ERROR: member not found {} while calling removeShardFromRoot(), returning root", memberId)
        root
      case Some(n) =>
        n.children = n.children.filterNot(_.name == shardId)
        root
    }
  }

  def removeEntity(root: Node, path: NodePath): Node = {
    findShardFromRoot(root, path.shardId) match {
      case None =>
        log.debug2("ERROR: bike $memberId ** {} ** {} not found while calling removeEntity(), returning root", path.shardId, path.entityId)
        root
      case Some(n) =>
        n.children = n.children.filterNot(_.name == path.entityId)
        if (n.children.isEmpty) removeShardFromRoot(root, path.memberId, path.shardId)
        root
    }
  }

  /**
   * Gathers all leaves into a list to return as aggregate.
   * Note: this is for demo purposes. If tree is large, this method is not efficient.
   * Todo: make it efficient.
   * @param root root from which to start counting
   * @return the list of leaves
   */
  def countLeaves(root: Node): List[Repr] = {
    var list = List[Repr]()
    for {
      member <- root.children
      shard <- member.children
      bike <- shard.children.map(_.representation())
    } list = bike :: list
    list
  }
}

