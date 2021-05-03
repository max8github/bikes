package akka.sample.bikes.tree

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._

/**
 * Alternative implementation for tree: https://stackoverflow.com/questions/28726176/generate-a-tree-in-scala
 * //  sealed trait Tree[+A]
 * //
 * //  case class Leaf[A](value: A) extends Tree[A]
 * //
 * //  case class Branch[A](children: List[Tree[A]]) extends Tree[A]
 *
 * //  def generate(p: Double): Tree[Int] = {
 * //    if (util.Random.nextDouble < p) {
 * //      val list = List()
 * //      Branch(generate(p), generate(p))
 * //    } else
 * //      Leaf(0)
 * //  }
 */
class JsonTreeTest extends AnyFlatSpec with Matchers {

  def findEntityWithPath(root: Node, path: NodePath): Option[Node] = {
    for {
      member <- root.children.find(_.name == path.memberId)
      shard <- member.children.find(_.name == path.shardId)
      bike <- shard.children.find(_.name == path.entityId)
    } yield bike
  }

  "Json Protocols" should "enable converting recursive Scala objects to a JSON AST and viceversa" in {
    import NodeProtocol._
    val member1 = Node("m1", "member")
    val member2Kids = Set(Node("shard1", "shard"), Node("shard2", "shard"))
    val member2 = Node(name = "m2", `type` = "member", children = member2Kids)
    val members = Set(member1, member2)
    val root = Node(name = "one", `type` = "cluster", children = members)
    val json = root.toJson
    println(s"json: ${json.prettyPrint}")
    val tree = json.convertTo[Node]
    tree should equal(root)

    //add member
    val member3 = Node("m3", "member")
    val newRoot = addOrUpdateMember(root, "m3", "MemberUp")
    println(s"\n\njson: ${newRoot.toJson.prettyPrint}")
    newRoot.children.find(_.name == "m3").get should equal(member3)

    //add shard
    val shard31 = Node("shard31", "shard")
    val newRootWithShard31 = addShardFromRoot(root, "m3", "shard31")
    println(s"\n\njson: ${newRootWithShard31.toJson.prettyPrint}")
    (for {
      member <- newRootWithShard31.children
      shard <- member.children
    } yield shard).find(_.name == "shard31").get should equal(shard31)

    //add bike
    val bike311 = Node("bike311", "entity")
    val newRootWithBike311 = addOrUpdateEntity(root, NodePath("m3", "shard31", "bike311"))
    println(s"\n\njson: ${newRootWithBike311.toJson.prettyPrint}")
    findEntityWithPath(newRootWithBike311, NodePath("m3", "shard31", "bike311")).get should equal(bike311)

    //remove bike
    val rootWithRemovedBike311 = removeEntity(root, NodePath("m3", "shard31", "bike311"))
    println(s"\n\njson with removed bike 311:\n${rootWithRemovedBike311.toJson.prettyPrint}")

    //remove shard
    val rootWithRemovedShard31 = removeShardFromRoot(root, "m3", "shard31")
    println(s"\n\njson with removed shard 31:\n${rootWithRemovedShard31.toJson.prettyPrint}")

    //remove member m3
    val rootWithRemovedMember3 = removeMember(root, "m3")
    println(s"\n\njson with removed member 3:\n${rootWithRemovedMember3.toJson.prettyPrint}")
  }
}
