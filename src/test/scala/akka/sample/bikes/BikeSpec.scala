package akka.sample.bikes

import akka.actor.testkit.typed.scaladsl.{ LogCapturing, ScalaTestWithActorTestKit }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.sample.bikes.Procurement.{ OpCompleted, SetMaxFailures, SetMode, SetSpeed, SomeOperation }
import akka.sample.bikes.Bike._
import akka.sample.bikes.tree.GlobalTreeActor
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps

class BikeSpec extends ScalaTestWithActorTestKit with WordSpecLike with LogCapturing {

  val numShards = ConfigFactory.defaultApplication().getInt("akka.cluster.sharding.number-of-shards")

  "Procurement" must {

    "get a response from the external service" in {
      val procurement = spawn(Procurement(system))
      procurement ! Procurement.SetSpeed(400L)
      val blueprint = Blueprint(NiUri("fac4c41e", "git@github"))
      val probe = createTestProbe[Procurement.Reply]()
      procurement ! SomeOperation(blueprint, probe.ref, "download")
      val msg = probe.receiveMessage(20.seconds)
      println(s"Received message: $msg")
    }

    "fail when there are too many failures from the external service" in {
      val procurement = spawn(Procurement(system))
      procurement ! Procurement.SetSpeed(4L)
      procurement ! Procurement.SetMode(false)
      procurement ! Procurement.SetMaxFailures(4)
      val blueprint = Blueprint(NiUri("af1ac4c41e", "git@github"))
      val probe = createTestProbe[Procurement.Reply]()
      procurement ! Procurement.SomeOperation(blueprint, probe.ref, "download")
      probe.expectMessageType[Procurement.OpFailed](20 seconds)
    }

    "react in the correct sequence of events when download is issued" in {
      val probe = createTestProbe[Procurement.Operation]()
      val mockedProcurement = spawn(Behaviors.monitor(probe.ref, Procurement(system)))
      mockedProcurement ! SetSpeed(40L)
      probe.expectMessageType[Procurement.Operation](3 seconds)
      mockedProcurement ! SetMaxFailures(1)
      probe.expectMessageType[Procurement.Operation](3 seconds)
      mockedProcurement ! SetMode(false)
      probe.expectMessageType[Procurement.Operation](3 seconds)
      val tree = spawn(GlobalTreeActor())
      val mockedShard = Behaviors.receiveMessage[ClusterSharding.ShardCommand] { _ =>
        Behaviors.same
      }
      val probeShard = spawn(mockedShard)

      val blueprint = Blueprint(NiUri("cc4c41e", "e1ea1e"))
      val bike = spawn(Bike(blueprint.makeEntityId(), mockedProcurement, tree, probeShard, numShards))
      bike ! DownloadCmd(blueprint)
      val msg = probe.expectMessageType[Procurement.SomeOperation](10 seconds)
      msg.name shouldBe "download()"
      bike ! CreateCmd(blueprint)
      val msg2 = probe.expectMessageType[Procurement.SomeOperation](10 seconds)
      msg2.name shouldBe "create()"
      bike ! ReserveCmd
      val msg3 = probe.expectMessageType[Procurement.SomeOperation](10 seconds)
      msg3.name shouldBe "reserve()"
    }

    "fsm to react in the correct sequence of events when download is issued" in {
      val procurement = spawn(Procurement(system))
      procurement ! Procurement.SetSpeed(4L)
      procurement ! Procurement.SetMaxFailures(4)
      procurement ! Procurement.SetMode(false)

      val tree = spawn(GlobalTreeActor())
      val mockedShard = Behaviors.receiveMessage[ClusterSharding.ShardCommand] { _ =>
        Behaviors.same
      }
      val clusterShard = spawn(mockedShard)

      val blueprint = Blueprint(NiUri("cc4c41e", "e1ea1e"))

      val probe = createTestProbe[Bike.Command]()
      val mockedBike = spawn(Behaviors.monitor(probe.ref, Bike(blueprint.makeEntityId(), procurement, tree, clusterShard, numShards)))

      mockedBike ! DownloadCmd(blueprint)
      probe.expectMessageType[Bike.DownloadCmd](10 seconds)
      //      val msg = probe.expectMessageType[Bike.AdaptedReply](10 seconds)
      //      msg.response shouldBe OpCompleted(blueprint)
    }
  }
}