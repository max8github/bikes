package akka.sample.bikes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import akka.{ Done, NotUsed }

import scala.concurrent.Future

object SingleWebSocketRequest extends App {

  private implicit val system = ActorSystem()
  import system.dispatcher

  // print each incoming strict text message
  val printSink: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: TextMessage.Strict =>
        println(message.text)
    }

  val helloSource: Source[Message, NotUsed] =
    Source.single(TextMessage("Coppi e Bartali"))

  // the Future[Done] is the materialized value of Sink.foreach
  // and it is completed when the stream completes
  val flow: Flow[Message, Message, Future[Done]] =
    Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)

  // upgradeResponse is a Future[WebSocketUpgradeResponse] that
  // completes or fails when the connection succeeds or fails
  // and closed is a Future[Done] representing the stream completion from above
  val (upgradeResponse, closed) =
    Http().singleWebSocketRequest(WebSocketRequest("ws://localhost:8080/events"), flow)

  val connected = upgradeResponse.map { upgrade =>
    // just like a regular http request we can access response status which is available via upgrade.response.status
    // status code 101 (Switching Protocols) indicates that server support WebSockets
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Done
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  // in a real application you would not side effect here
  // and handle errors more carefully
  connected.onComplete(println)
  closed.foreach(_ => println("eccolo"))
}
