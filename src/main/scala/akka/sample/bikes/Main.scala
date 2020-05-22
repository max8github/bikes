package akka.sample.bikes

object Main {

  def main(args: Array[String]): Unit = {

    import akka.actor.typed.ActorSystem
    import akka.actor.typed.scaladsl.Behaviors
    import com.typesafe.config.ConfigFactory

    val rootBehavior = Behaviors.setup[Nothing] { _ => Behaviors.empty }
    ActorSystem[Nothing](rootBehavior, "BikeService", ConfigFactory.load("application_local.conf"))
  }
}
