package akka.sample

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
  }
}
