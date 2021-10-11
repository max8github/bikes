package akka.sample

import akka.actor.typed.ActorSystem
import akka.cluster.typed.Cluster
import akka.sample.bikes.tree.NodePath
import akka.actor.typed.ActorRef
import akka.sample.bikes.tree.Repr
import com.fasterxml.jackson.annotation.{ JsonSubTypes, JsonTypeInfo, JsonTypeName }
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import spray.json.{ JsObject, JsString, JsValue }
import spray.json._

package object bikes {
  import akka.cluster.sharding.typed.{ ShardingEnvelope, ShardingMessageExtractor }

  /**
   * This interface defines all the commands that the persistent actor supports.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[Idle], name = "idle"),
      new JsonSubTypes.Type(value = classOf[GoodBye], name = "goodbye"),
      new JsonSubTypes.Type(value = classOf[DownloadCmd], name = "downloadCmd"),
      new JsonSubTypes.Type(value = classOf[CreateCmd], name = "createCmd"),
      new JsonSubTypes.Type(value = classOf[ReserveCmd], name = "reserveCmd"),
      new JsonSubTypes.Type(value = classOf[YieldCmd], name = "yieldCmd"),
      new JsonSubTypes.Type(value = classOf[KickCmd], name = "kickCmd"),
      new JsonSubTypes.Type(value = classOf[GetStateCmd], name = "getStateCmd"),
      new JsonSubTypes.Type(value = classOf[Timeout], name = "timeout")))
  sealed trait Command extends CborSerializable

  @JsonDeserialize(`using` = classOf[IdleDeserializer])
  sealed trait Idle extends Command
  @JsonTypeName("idle")
  final case object Idle extends Idle
  class IdleDeserializer extends StdDeserializer[Idle](Idle.getClass) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): Idle = Idle
  }
  @JsonDeserialize(`using` = classOf[GoodByeDeserializer])
  sealed trait GoodBye extends Command
  @JsonTypeName("goodBye")
  final case object GoodBye extends GoodBye
  class GoodByeDeserializer extends StdDeserializer[GoodBye](GoodBye.getClass) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): GoodBye = GoodBye
  }
  final case class DownloadCmd(blueprint: Blueprint) extends Command
  final case class CreateCmd(blueprint: Blueprint) extends Command

  @JsonDeserialize(`using` = classOf[ReserveCmdDeserializer])
  sealed trait ReserveCmd extends Command
  @JsonTypeName("reserveCmd")
  final case object ReserveCmd extends ReserveCmd
  class ReserveCmdDeserializer extends StdDeserializer[ReserveCmd](ReserveCmd.getClass) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): ReserveCmd = ReserveCmd
  }

  @JsonDeserialize(`using` = classOf[YieldCmdDeserializer])
  sealed trait YieldCmd extends Command
  @JsonTypeName("yieldCmd")
  final case object YieldCmd extends YieldCmd
  class YieldCmdDeserializer extends StdDeserializer[YieldCmd](YieldCmd.getClass) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): YieldCmd = YieldCmd
  }

  @JsonDeserialize(`using` = classOf[KickCmdDeserializer])
  sealed trait KickCmd extends Command
  @JsonTypeName("kickCmd")
  final case object KickCmd extends KickCmd
  class KickCmdDeserializer extends StdDeserializer[KickCmd](KickCmd.getClass) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): KickCmd = KickCmd
  }

  final case class GetStateCmd(bikeId: String, replyTo: ActorRef[BikeRoutesSupport.StatusResponse]) extends Command

  @JsonDeserialize(`using` = classOf[TimeoutDeserializer])
  sealed trait Timeout extends Command
  @JsonTypeName("timeout")
  final case object Timeout extends Timeout
  class TimeoutDeserializer extends StdDeserializer[Timeout](Timeout.getClass) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): Timeout = Timeout
  }

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

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[InitState], name = "initState"),
      new JsonSubTypes.Type(value = classOf[DownloadingState], name = "downloadingState"),
      new JsonSubTypes.Type(value = classOf[DownloadedState], name = "downloadedState"),
      new JsonSubTypes.Type(value = classOf[CreatingState], name = "creatingState"),
      new JsonSubTypes.Type(value = classOf[CreatedState], name = "createdState"),
      new JsonSubTypes.Type(value = classOf[ReservingState], name = "reservingState"),
      new JsonSubTypes.Type(value = classOf[ReservedState], name = "reservedState"),
      new JsonSubTypes.Type(value = classOf[YieldingState], name = "yieldingState"),
      new JsonSubTypes.Type(value = classOf[YieldedState], name = "yieldedState"),
      new JsonSubTypes.Type(value = classOf[ErrorState], name = "errorState")))
  sealed trait State extends CborSerializable

  @JsonDeserialize(`using` = classOf[InitStateDeserializer])
  sealed trait InitState extends State
  @JsonTypeName("initState")
  case object InitState extends InitState
  class InitStateDeserializer extends StdDeserializer[InitState](InitState.getClass) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): InitState = InitState
  }
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

    implicit val queryStatusFormat = jsonFormat2(BikeRoutesSupport.StatusResponse)
  }

  object BikeRoutesSupport extends SprayJsonSupport {
    sealed trait Status extends CborSerializable
    final case class Inventory(entities: List[Repr]) extends Status
    final case class StatusResponse(bikeId: String, state: String) extends Status
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
