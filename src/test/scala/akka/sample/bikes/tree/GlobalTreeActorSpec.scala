package akka.sample.bikes.tree

import akka.actor.testkit.typed.scaladsl.{ BehaviorTestKit, TestInbox }
import akka.sample.bikes.tree.NodeProtocol._
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import spray.json._

class GlobalTreeActorSpec extends Matchers with AnyFunSpecLike
  with BeforeAndAfterAll {

  describe("The Global Tree") {

    it("should build correctly") {
      val expectedTree = """{"name":"cluster","type":"cluster","nodeState":"","children":[{"name":"bikes","type":"member","nodeState":"","children":[{"name":"shard1","type":"shard","nodeState":"","children":[{"name":"e1","type":"entity","nodeState":"InitState","children":[]},{"name":"e2","type":"entity","nodeState":"ReservedState","children":[]}]}]}]}"""
      val jsvExpected = expectedTree.parseJson
      val nExpected = jsvExpected.convertTo[Node]
      val testKit = BehaviorTestKit(GlobalTreeActor())
      testKit.run(GlobalTreeActor.AddMember("bikes", "MemberUp"))
      testKit.run(GlobalTreeActor.AddEntity(NodePath("bikes", "shard1", "e1")))
      testKit.run(GlobalTreeActor.AddEntity(NodePath("bikes", "shard1", "e2")))
      val inbox = TestInbox[Node]()
      testKit.run(GlobalTreeActor.GetJson(inbox.ref))
      val n = inbox.receiveMessage()
      println(s" tree: $n")
      val json = n.toJson
      println(s" json: $json")

      n mustEqual nExpected
    }
  }
}