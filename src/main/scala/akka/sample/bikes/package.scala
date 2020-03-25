package akka.sample

import spray.json.{ DeserializationException, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat }
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
   * Marker trait for serialization with Jackson CBOR. Currently (Mar 2020) unused.
   * Turn this back on and remove Java Serialization in `application.conf` once Cassandra is
   * upgraded to support typed actors and the newest version of Akka.
   */
  trait CborSerializable

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
  import Bike.{ Blueprint, NiUri }
  import akka.sample.bikes.BikeRoutes.Inventory
  import spray.json.DefaultJsonProtocol

  object JsonSupport extends SprayJsonSupport {
    import DefaultJsonProtocol._
    import akka.sample.bikes.tree.NodeProtocol._
    implicit val coordsFormat = jsonFormat2(NiUri)
    implicit val blueprintFormat = jsonFormat4(Blueprint)
    implicit val inventoryFormat = jsonFormat1(Inventory)

    implicit object InitStateJsonFormat extends JsonFormat[Bike.InitState.type] {
      def write(c: Bike.InitState.type) = JsObject("state" -> JsString("init"))
      def read(value: JsValue): Bike.InitState.type = {
        value.asJsObject.getFields("state") match {
          case Seq(JsString(_)) => Bike.InitState
          case _ => throw new DeserializationException("InitState expected")
        }
      }
    }
    implicit val downloadingFormat = jsonFormat1(Bike.DownloadingState)
    implicit val downloadedFormat = jsonFormat1(Bike.DownloadedState)
    implicit val creatingFormat = jsonFormat1(Bike.CreatingState)
    implicit val createdFormat = jsonFormat2(Bike.CreatedState)
    implicit val reservingFormat = jsonFormat2(Bike.ReservingState)
    implicit val yieldingFormat = jsonFormat2(Bike.YieldingState)

    implicit object ErrorStateJsonFormat extends JsonFormat[Bike.ErrorState] {
      def write(c: Bike.ErrorState) = JsObject(
        "name" -> JsString("ErrorState"),
        "msg" -> JsString(c.msg),
        "offendingCommand" -> JsString(c.offendingCommand.getClass.getSimpleName),
        "lastState" -> JsString(c.lastState.getClass.getSimpleName))
      def read(value: JsValue): Bike.ErrorState = {
        value.asJsObject.getFields("msg", "offendingCommand", "lastState") match {
          case Seq(JsString(msg), JsString(_), JsString(_)) => Bike.ErrorState(msg, Bike.KickCmd, Bike.InitState)
          case _ => throw new DeserializationException("ErrorState expected")
        }
      }
    }

    implicit object ReservedStateJsonFormat extends JsonFormat[Bike.ReservedState] {
      def write(c: Bike.ReservedState) = JsObject(
        "name" -> JsString("ReservedState"),
        "location" -> c.location.toJson)
      def read(value: JsValue): Bike.ReservedState = {
        value.asJsObject.getFields("blueprint", "location") match {
          case Seq(blueprint, location) => Bike.ReservedState(blueprint.convertTo[Bike.Blueprint], location.convertTo[Bike.NiUri])
          case _ => throw DeserializationException("ReservedState expected")
        }
      }
    }

    implicit object YieldedStateJsonFormat extends JsonFormat[Bike.YieldedState] {
      def write(c: Bike.YieldedState) = JsObject(
        "name" -> JsString("YieldedState"),
        "location" -> c.location.toJson)
      def read(value: JsValue): Bike.YieldedState = {
        value.asJsObject.getFields("blueprint", "location") match {
          case Seq(blueprint, location) => Bike.YieldedState(blueprint.convertTo[Bike.Blueprint], location.convertTo[Bike.NiUri])
          case _ => throw DeserializationException("YieldedState expected")
        }
      }
    }

    implicit object StateFormat extends RootJsonFormat[Bike.State] {
      def write(obj: Bike.State): JsValue =
        JsObject((obj match {
          case Bike.InitState => JsObject("name" -> JsString("InitState"))
          case c: Bike.DownloadingState => JsObject("name" -> JsString("DownloadingState"))
          case c: Bike.DownloadedState => JsObject("name" -> JsString("DownloadedState"))
          case c: Bike.CreatingState => JsObject("name" -> JsString("CreatingState"))
          case c: Bike.CreatedState => JsObject("name" -> JsString("CreatedState"))
          case c: Bike.ReservingState => JsObject("name" -> JsString("ReservingState"))
          case c: Bike.ReservedState => c.toJson
          case c: Bike.YieldingState => JsObject("name" -> JsString("YieldingState"))
          case c: Bike.YieldedState => c.toJson
          case a: Bike.ErrorState => a.toJson
          case unknown => deserializationError(s"json deserialize error: $unknown")
        }).asJsObject.fields)

      def read(json: JsValue): Bike.State =
        json.asJsObject.getFields("name") match {
          case Seq(JsString("DownloadingState")) => json.convertTo[Bike.DownloadingState]
          case Seq(JsString("DownloadedState")) => json.convertTo[Bike.DownloadedState]
          case Seq(JsString("CreatingState")) => json.convertTo[Bike.CreatingState]
          case Seq(JsString("CreatedState")) => json.convertTo[Bike.CreatedState]
          case Seq(JsString("ReservingState")) => json.convertTo[Bike.ReservingState]
          case Seq(JsString("ReservedState")) => json.convertTo[Bike.ReservedState]
          case Seq(JsString("YieldingState")) => json.convertTo[Bike.YieldingState]
          case Seq(JsString("YieldedState")) => json.convertTo[Bike.YieldedState]
          case Seq(JsString("ErrorState")) => json.convertTo[Bike.ErrorState]
          case unrecognized => serializationError(s"json serialization error $unrecognized")
        }
    }

    implicit val queryStatusFormat = jsonFormat2(BikeRoutes.QueryStatus)

  }
}
