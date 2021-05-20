package akka.sample

import akka.actor.typed.ActorSystem
import akka.cluster.typed.Cluster
import akka.sample.bikes.tree.NodePath
import akka.actor.typed.ActorRef
import akka.sample.bikes.tree.Repr
import spray.json.{ JsObject, JsString, JsValue }
import spray.json._

package object bikes {
  import akka.cluster.sharding.typed.{ ShardingEnvelope, ShardingMessageExtractor }

  /**
   * This interface defines all the commands that the persistent actor supports.
   */
  sealed trait Command extends CborSerializable
  final case object Idle extends Command
  final case object GoodBye extends Command
  final case class DownloadCmd(blueprint: Blueprint) extends Command
  final case class CreateCmd(blueprint: Blueprint) extends Command
  final case object ReserveCmd extends Command
  final case object YieldCmd extends Command
  final case object KickCmd extends Command
  final case class GetStateCmd(bikeId: String, replyTo: ActorRef[BikeRoutesSupport.QueryStatus]) extends Command
  final case object Timeout extends Command

  /**
   * The purpose of a wrapping class like this is to avoid circular dependencies between sender and receiver actors.
   * Here, `Bike` depends on an external service, `Procurement`, because it needs to send messages to `Procurement`:
   * ```
   * bike ! Procurement.SomeOperation
   * ```
   * `Procurement` needs to respond to those messages:
   * ```
   * bike ! Bike.Response
   * ```
   * causing `Procurement` to depend on `Bike`.
   *
   * Instead of depending on `Bike` that way, we can use an adapter and have `Procurement` do: `actorRef ! Procurement.Reply`,
   * where actorRef is meant to be a bike.
   *
   * Here is how it goes: the bike sends a message to `Procurement`:
   * ```
   * procurement ! Procurement.SomeOperation(cmd.blueprint, replyToMapper, "download()")
   * ```
   * where `replyToMapper` is this:
   * ```
   * val replyToMapper: ActorRef[Reply] = context.messageAdapter(reply => AdaptedReply(reply))
   * ```
   * All `Procurement` knows is that it replies to an `ActorRef[Reply]`, where `Reply` is defined by `Procurement`.
   * In reality, `Procurement` sends a message to an adapter, `replyMapper`, which will take a `Reply` and transform it into a
   * `AdaptedReply`, which is something understood by `Bike`. So, in the end, all of this happens as if `Procurement`
   * sent a `AdaptedReply` message to `Bike`, as if it were: `procurement ! Bike.AdaptedReply`.
   * There is just an adapter in the middle.
   *
   * @param response response from external service
   */
  final case class AdaptedReply(response: Reply) extends akka.sample.bikes.Command

  sealed trait Reply extends CborSerializable
  case class OpCompleted(blueprint: Blueprint) extends Reply
  case class OpFailed(blueprint: Blueprint, reason: String) extends Reply

  sealed trait State extends CborSerializable
  final case object InitState extends State
  final case class DownloadingState(blueprint: Blueprint) extends State
  final case class DownloadedState(blueprint: Blueprint) extends State
  final case class CreatingState(blueprint: Blueprint) extends State
  final case class CreatedState(blueprint: Blueprint, location: NiUri = NiUri("e7701e-1ea1e", "https://www.bikes.com/location}")) extends State
  final case class ReservingState(blueprint: Blueprint, location: NiUri) extends State
  final case class ReservedState(blueprint: Blueprint, location: NiUri) extends State
  final case class YieldingState(blueprint: Blueprint, location: NiUri) extends State
  final case class YieldedState(blueprint: Blueprint, location: NiUri) extends State
  final case class ErrorState(msg: String, offendingCommand: Command, lastState: State) extends State {
    override def toString: String = s"ErrorState('$msg', ${offendingCommand.getClass.getSimpleName}, ${lastState.getClass.getSimpleName})"
  }

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
  final class BikeMessageExtractor(val numberOfShards: Int) extends ShardingMessageExtractor[ShardingEnvelope[Command], Command] {
    import BikeMessageExtractor._
    override def entityId(envelope: ShardingEnvelope[Command]): String = envelope.entityId
    override def shardId(entityId: String): String = consHash(entityId, numberOfShards)
    override def unwrapMessage(envelope: ShardingEnvelope[Command]): Command = envelope.message
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

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
  import akka.sample.bikes.BikeRoutesSupport.Inventory
  import spray.json.DefaultJsonProtocol

  object JsonSupport extends SprayJsonSupport {
    import DefaultJsonProtocol._
    import akka.sample.bikes.tree.NodeProtocol._
    implicit val coordsFormat = jsonFormat2(NiUri)
    implicit val blueprintFormat = jsonFormat4(Blueprint)
    implicit val inventoryFormat = jsonFormat1(Inventory)

    implicit object InitStateJsonFormat extends JsonWriter[InitState.type] {
      def write(c: InitState.type) = JsObject("state" -> JsString("init"))
    }
    implicit val downloadingFormat = jsonFormat1(DownloadingState)
    implicit val downloadedFormat = jsonFormat1(DownloadedState)
    implicit val creatingFormat = jsonFormat1(CreatingState)
    implicit val createdFormat = jsonFormat2(CreatedState)
    implicit val reservingFormat = jsonFormat2(ReservingState)
    implicit val yieldingFormat = jsonFormat2(YieldingState)

    implicit object ErrorStateJsonFormat extends JsonWriter[ErrorState] {
      def write(c: ErrorState) = JsObject(
        "name" -> JsString("ErrorState"),
        "msg" -> JsString(c.msg),
        "offendingCommand" -> JsString(c.offendingCommand.getClass.getSimpleName),
        "lastState" -> JsString(c.lastState.getClass.getSimpleName))
    }

    implicit object ReservedStateJsonFormat extends JsonWriter[ReservedState] {
      def write(c: ReservedState) = JsObject(
        "name" -> JsString("ReservedState"),
        "location" -> c.location.toJson)
    }

    implicit object YieldedStateJsonFormat extends JsonWriter[YieldedState] {
      def write(c: YieldedState) = JsObject(
        "name" -> JsString("YieldedState"),
        "location" -> c.location.toJson)
    }

    implicit object StateFormat extends RootJsonWriter[State] {
      def write(obj: State): JsValue =
        JsObject((obj match {
          case InitState => JsObject("name" -> JsString("InitState"))
          case _: DownloadingState => JsObject("name" -> JsString("DownloadingState"))
          case _: DownloadedState => JsObject("name" -> JsString("DownloadedState"))
          case _: CreatingState => JsObject("name" -> JsString("CreatingState"))
          case _: CreatedState => JsObject("name" -> JsString("CreatedState"))
          case _: ReservingState => JsObject("name" -> JsString("ReservingState"))
          case state: ReservedState => state.toJson
          case _: YieldingState => JsObject("name" -> JsString("YieldingState"))
          case state: YieldedState => state.toJson
          case state: ErrorState => state.toJson
          case unknown => deserializationError(s"json deserialize error: $unknown")
        }).asJsObject.fields)

    }

    implicit object QueryStatusFormat extends RootJsonWriter[BikeRoutesSupport.QueryStatus] {
      def write(c: BikeRoutesSupport.QueryStatus) = JsObject(
        "bikeId" -> JsString(c.bikeId),
        "state" -> c.state.toJson)
    }

  }

  object BikeRoutesSupport extends SprayJsonSupport {
    sealed trait Status extends CborSerializable
    final case class Inventory(entities: List[Repr]) extends Status
    final case class QueryStatus(bikeId: String, state: State) extends Status
  }

  /** Represents the coordinates of a resource, the unique way to identify a certain resource like blueprint parts. */
  final case class NiUri(version: String, location: String) extends CborSerializable
  type Token = String

  def displayOfId(bikeId: String): String = {
    val index = bikeId.lastIndexOf("-")
    bikeId.substring(0, if (index != -1) index else bikeId.length)
  }
  import JsonSupport._
  final case class Blueprint(instructions: NiUri, bom: NiUri = NiUri("", ""), mechanic: NiUri = NiUri("", ""),
    access: Token = "") extends CborSerializable {
    def displayId: String = displayOfId(instructions.version)
    def makeEntityId(): String = instructions.toJson.convertTo[NiUri].version
  }
}
