package akka.sample

import akka.actor.typed.ActorSystem
import akka.cluster.typed.Cluster
import akka.sample.bikes.Bike.Blueprint
import akka.sample.bikes.tree.NodePath
import spray.json.{ JsObject, JsString, JsValue }
import spray.json._

package object bikes {
  import akka.cluster.sharding.typed.{ ShardingEnvelope, ShardingMessageExtractor }

  object BikeMessageExtractor {
    def consHash(entityId: String, numberOfShards: Int): String = math.abs(entityId.hashCode % numberOfShards).toString
    def apply(numberOfShards: Int) = new BikeMessageExtractor(numberOfShards)
  }

  /**
   * This class is here, not because a special hashing is needed (it is the same as the default), but
   * because of method `BikeMessageExtractor#consHash()`, which needs to be reused consistently by method
   * `fullPath` in class `Bike`.
   * @param numberOfShards
   */
  final class BikeMessageExtractor(val numberOfShards: Int) extends ShardingMessageExtractor[ShardingEnvelope[Bike.Command], Bike.Command] {
    import BikeMessageExtractor._
    override def entityId(envelope: ShardingEnvelope[Bike.Command]): String = envelope.entityId
    override def shardId(entityId: String): String = consHash(entityId, numberOfShards)
    override def unwrapMessage(envelope: ShardingEnvelope[Bike.Command]): Bike.Command = envelope.message
  }

  /**
   * Finds (memberId, shardId, bikeId) given bikeId and ActorSystem.
   * Shard id is easily found from the entity id by using the sharding function.
   * Member id is known from the system.
   *
   * The tree model in GlobalTreeActor is not the real model (the cluster), but a copy
   * of it. It would be great to give d3.js the correct model from jmx or something else, but from the cluster itself,
   * without having to create a Tree copy structure.
   *
   * @param bikeId entity id for the bike
   * @param system actor system
   * @return
   */
  def fullPath(bikeId: String, system: ActorSystem[_])(implicit numOfShards: Int): NodePath = {
    val shardId = BikeMessageExtractor.consHash(bikeId, numOfShards)
    val memberId = Cluster.get(system).selfMember.address.toString
    NodePath(memberId, shardId, bikeId)
  }
  def fullPath(blueprint: Blueprint, system: ActorSystem[_])(implicit numOfShards: Int): NodePath = {
    val bikeId = blueprint.makeEntityId()
    fullPath(bikeId, system)
  }

  /**
   * Marker trait for serialization with Jackson CBOR. Currently (Mar 2020) unused.
   * Turn this back on and remove Java Serialization in `application.conf` once Cassandra is
   * upgraded to support typed actors and the newest version of Akka.
   */
  trait CborSerializable

  /** Represents the coordinates of a resource, the unique way to identify a certain resource like blueprint parts. */
  final case class NiUri(version: String, location: String)

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
  import Bike.Blueprint
  import akka.sample.bikes.BikeRoutes.Inventory
  import spray.json.DefaultJsonProtocol

  object JsonSupport extends SprayJsonSupport {
    import DefaultJsonProtocol._
    import akka.sample.bikes.tree.NodeProtocol._
    implicit val coordsFormat = jsonFormat2(NiUri)
    implicit val blueprintFormat = jsonFormat4(Blueprint)
    implicit val inventoryFormat = jsonFormat1(Inventory)

    implicit object InitStateJsonFormat extends JsonWriter[Bike.InitState.type] {
      def write(c: Bike.InitState.type) = JsObject("state" -> JsString("init"))
    }
    implicit val downloadingFormat = jsonFormat1(Bike.DownloadingState)
    implicit val downloadedFormat = jsonFormat1(Bike.DownloadedState)
    implicit val creatingFormat = jsonFormat1(Bike.CreatingState)
    implicit val createdFormat = jsonFormat2(Bike.CreatedState)
    implicit val reservingFormat = jsonFormat2(Bike.ReservingState)
    implicit val yieldingFormat = jsonFormat2(Bike.YieldingState)

    implicit object ErrorStateJsonFormat extends JsonWriter[Bike.ErrorState] {
      def write(c: Bike.ErrorState) = JsObject(
        "name" -> JsString("ErrorState"),
        "msg" -> JsString(c.msg),
        "offendingCommand" -> JsString(c.offendingCommand.getClass.getSimpleName),
        "lastState" -> JsString(c.lastState.getClass.getSimpleName))
    }

    implicit object ReservedStateJsonFormat extends JsonWriter[Bike.ReservedState] {
      def write(c: Bike.ReservedState) = JsObject(
        "name" -> JsString("ReservedState"),
        "location" -> c.location.toJson)
    }

    implicit object YieldedStateJsonFormat extends JsonWriter[Bike.YieldedState] {
      def write(c: Bike.YieldedState) = JsObject(
        "name" -> JsString("YieldedState"),
        "location" -> c.location.toJson)
    }

    implicit object StateFormat extends RootJsonWriter[Bike.State] {
      def write(obj: Bike.State): JsValue =
        JsObject((obj match {
          case Bike.InitState => JsObject("name" -> JsString("InitState"))
          case _: Bike.DownloadingState => JsObject("name" -> JsString("DownloadingState"))
          case _: Bike.DownloadedState => JsObject("name" -> JsString("DownloadedState"))
          case _: Bike.CreatingState => JsObject("name" -> JsString("CreatingState"))
          case _: Bike.CreatedState => JsObject("name" -> JsString("CreatedState"))
          case _: Bike.ReservingState => JsObject("name" -> JsString("ReservingState"))
          case state: Bike.ReservedState => state.toJson
          case _: Bike.YieldingState => JsObject("name" -> JsString("YieldingState"))
          case state: Bike.YieldedState => state.toJson
          case state: Bike.ErrorState => state.toJson
          case unknown => deserializationError(s"json deserialize error: $unknown")
        }).asJsObject.fields)

    }

    implicit object QueryStatusFormat extends RootJsonWriter[BikeRoutes.QueryStatus] {
      def write(c: BikeRoutes.QueryStatus) = JsObject(
        "bikeId" -> JsString(c.bikeId),
        "state" -> c.state.toJson)
    }

  }
}
