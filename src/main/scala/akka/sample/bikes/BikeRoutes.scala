package akka.sample.bikes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Route

private[bikes] final class BikeRoutes()(implicit system: ActorSystem[_]) {
  import akka.http.scaladsl.server.Directives._

  val work: Route =
    pathPrefix("monitor") {
      getFromResource("monitor.html", ContentTypes.`text/html(UTF-8)`)
    }
}
