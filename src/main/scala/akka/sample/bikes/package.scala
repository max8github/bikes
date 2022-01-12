package akka.sample

import akka.actor.typed.ActorRef

package object bikes {
  trait Command extends CborSerializable
  final case class DownloadCmd(blueprint: String) extends Command
  final case class GetStateCmd(bikeId: String, replyTo: ActorRef[String]) extends Command
  case class OpCompleted(blueprint: String) extends Command
  case class OpFailed(blueprint: String, reason: String) extends Command

  sealed trait State extends CborSerializable
  case object InitState extends State
  final case class DownloadingState(blueprint: String) extends State
  final case class DownloadedState(blueprint: String) extends State
  final case class ErrorState(msg: String, offendingCommand: Command, lastState: State) extends State
}
