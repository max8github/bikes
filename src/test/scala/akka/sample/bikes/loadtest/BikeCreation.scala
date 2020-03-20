package akka.sample.bikes.loadtest

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.Predef.http

import scala.concurrent.duration._

class BikeCreation extends Simulation {

  lazy val urlString = "http://127.0.0.1:8084"

  lazy val httpConfig = http.baseUrl(s"$urlString")

  lazy val headers = Map(
    "accept-language" -> "en-US,en;q=0.9,it;q=0.8",
    "content-type" -> "application/json",
    "accept" -> "application/json, text/plain, */*")

  /** Used to create random blueprints (see bike.json) */
  lazy val generateUuid: Expression[Session] = _.set("randomId", UUID.randomUUID())

  lazy val createBike = http("create bike")
    .post("/bike")
    .headers(headers)
    .body(ElFileBody("src/test/resources/bike.json")).asJson

  setUp(scenario("bike creations")
    .exec(generateUuid)
    .exec(createBike)
    .inject(
      //      constantUsersPerSec(2).during(10.seconds)
      rampUsersPerSec(1) to 5 during (20.seconds)
    //
    )
    .protocols(httpConfig))
}
