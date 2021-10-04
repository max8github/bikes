package akka.sample.bikes

import akka.actor.Props
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.{ actor => classic }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage }
import akka.http.scaladsl.server.Directives.{ handleWebSocketMessages, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.pattern.ask
import akka.sample.bikes.tree.GlobalTreeActor
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{ Flow, GraphDSL, Keep, Sink, Source }
import akka.stream.{ CompletionStrategy, FlowShape, OverflowStrategy }
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Random, Success }

object WebSocketRoutes {

  case object GetWebsocketFlow

  object ClientHandlerActor {
    def props(treeActor: ActorRef[GlobalTreeActor.TreeCommand]): Props = classic.Props(new ClientHandlerActor(treeActor))
  }

  //todo: change to typed actor (not easy, because of the streaming and http here...)
  class ClientHandlerActor(treeActor: ActorRef[GlobalTreeActor.TreeCommand]) extends classic.Actor {

    import akka.actor.typed.scaladsl.adapter._

    implicit val as = context.system
    implicit val ex = as.dispatcher

    var treeString = """{"name":"cluster","type":"cluster"}"""

    val completion: PartialFunction[Any, CompletionStrategy] = {
      case akka.actor.Status.Success(s: CompletionStrategy) => s
      case akka.actor.Status.Success(_) => CompletionStrategy.Draining
      case akka.actor.Status.Success => CompletionStrategy.Draining
    }
    val failure: PartialFunction[Any, Throwable] = {
      case akka.actor.Status.Failure(cause) => cause
    }
    val (down, publisher) = Source.actorRef[String](completion, failure, 1000, OverflowStrategy.fail)
      .toMat(Sink.asPublisher(fanout = false))(Keep.both)
      .run()

    override def receive: Receive = {

      case GetWebsocketFlow =>
        val flow = Flow.fromGraph(GraphDSL.create() { implicit b =>
          val textMsgFlow = b.add(Flow[Message]
            .mapAsync(1) {
              case tm: TextMessage => tm.toStrict(3.seconds).map(_.text)
              case bm: BinaryMessage =>
                // consume the stream
                bm.dataStream.runWith(Sink.ignore)
                Future.failed(new Exception("ouch"))
            })

          val pubSrc = b.add(Source.fromPublisher(publisher).map(TextMessage(_)))

          textMsgFlow ~> Sink.foreach[String](self ! _)
          FlowShape(textMsgFlow.in, pubSrc.out)
        })
        sender() ! flow

      case s: String if s == "hi" =>
        treeActor ! GlobalTreeActor.GetJson(self)
        down ! treeString

      case s: String =>
        treeString = s
    }
  }

}
final class WebSocketRoutes(treeActor: ActorRef[GlobalTreeActor.TreeCommand])(implicit context: ActorContext[_]) {

  import WebSocketRoutes._
  import akka.actor.typed.scaladsl.adapter._

  implicit val as: classic.ActorSystem = context.system.toClassic
  implicit val timeout: Timeout = context.system.settings.config.getDuration("bikes.routes.ask-timeout").toMillis.millis

  lazy val websocketRoute: Route =
    path("events") {
      val handler = context.actorOf(ClientHandlerActor.props(treeActor))
      val futureFlow = (handler ? GetWebsocketFlow)(3.seconds).mapTo[Flow[Message, Message, _]]

      onComplete(futureFlow) {
        case Success(flow) => handleWebSocketMessages(flow)
        case Failure(err) => complete(err.toString)
      }
    }
}

