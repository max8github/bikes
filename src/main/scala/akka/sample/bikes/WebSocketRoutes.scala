package akka.sample.bikes

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.server.Directives.{ handleWebSocketMessages, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.sample.bikes.tree.GlobalTreeActor
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import akka.{ actor => classic }

import scala.concurrent.duration._

final class WebSocketRoutes(treeActor: ActorRef[GlobalTreeActor.TreeCommand])(implicit context: ActorContext[_]) {

  import akka.actor.typed.scaladsl.adapter._

  implicit val as: classic.ActorSystem = context.system.toClassic
  implicit val timeout: Timeout = context.system.settings.config.getDuration("bikes.routes.ask-timeout").toMillis.millis

  lazy val websocketRoute: Route =
    path("events") {
      handleWebSocketMessages(treeFlow)
    }

  lazy val treeFlow = ActorFlow.ask(treeActor)((_: Message, replyTo: ActorRef[String]) => GlobalTreeActor.GetJson(replyTo)).map(TextMessage(_))
}
