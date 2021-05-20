package akka.sample.bikes

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.{ actor => classic }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Random, Success }

/**
 * Actor in charge of calling external services.
 * External services are mimicked here by fake futures.
 */
object Procurement {
  sealed trait Operation extends CborSerializable
  trait AdminOperation extends Operation
  trait UserOperation extends Operation {
    val blueprint: Blueprint
    val fsm: ActorRef[Reply]
    val name: String
  }
  case class SomeOperation(blueprint: Blueprint, fsm: ActorRef[Reply], name: String) extends UserOperation
  case class SomeHickupsOperation(blueprint: Blueprint, fsm: ActorRef[Reply], name: String) extends UserOperation
  case class SetMaxFailures(failCount: Int) extends AdminOperation
  case class SetMode(isRandom: Boolean) extends AdminOperation
  case class SetSpeed(processingTime: Long) extends AdminOperation

  val random = new Random()
  /** Sets the number of times the external service will fail consecutively before responding. Useful for simulating retries. */
  val MAX_FAILURES = 2
  /** Number of times this actor is willing to try a given operation against an external service. */
  val ATTEMPTS = 3
  /** Processing time of an external operation. Mimics long response times from an external service. */
  val PROCESSING_TIME = 4000L
  val IS_RANDOM_MODE = true

  def apply(system: ActorSystem[_]): Behavior[Operation] = {
    import akka.actor.typed.scaladsl.adapter._
    implicit val classicSystem: classic.ActorSystem = system.toClassic
    implicit val sa = classicSystem.scheduler

    Behaviors.setup[Operation] { context =>

      def active(maxFailures: Int, processingTime: Long, isRandom: Boolean): Behavior[Operation] = {

        var currentFailures = 0

        /**
         *
         * @param name
         * @param blueprint
         * @param executionContext
         * @param scheduler
         * @return
         */
        def callExternalServiceWithHickups(name: String, blueprint: Blueprint)(implicit
          executionContext: ExecutionContext,
          scheduler: akka.actor.typed.Scheduler) =
          if (currentFailures < maxFailures) {
            currentFailures += 1
            Future.failed {
              Thread.sleep(PROCESSING_TIME)
              new IllegalStateException(currentFailures.toString)
            }
          } else {
            currentFailures = 0
            Future.successful {
              Thread.sleep(PROCESSING_TIME)
              blueprint
            }
          }

        def callExternalService(name: String, blueprint: Blueprint)(implicit
          executionContext: ExecutionContext,
          scheduler: akka.actor.typed.Scheduler) = {
          val ok = random.nextBoolean()
          if (!ok) Future.failed {
            Thread.sleep(PROCESSING_TIME)
            new IllegalStateException(currentFailures.toString)
          }
          else Future.successful {
            Thread.sleep(PROCESSING_TIME)
            blueprint
          }
        }

        Behaviors.receiveMessage[Operation] {
          case sop: UserOperation =>
            implicit val ex = context.executionContext
            implicit val sc = context.system.scheduler
            val (blueprint, replyTo, name) = (sop.blueprint, sop.fsm, sop.name)
            // todo: use SupervisorStrategy.restart.withLimit(maxNrOfRetries = 2, withinTimeRange) instead (see akka.actor.typed.SupervisionSpec for an example)
            val f: (String, Blueprint) => Future[Blueprint] = (name, blueprint) => if (isRandom) callExternalService(name, blueprint) else callExternalServiceWithHickups(name, blueprint)
            akka.pattern.retry(() => f(name, blueprint), ATTEMPTS, 100 milliseconds)
              .onComplete {
                case Success(_) =>
                  replyTo ! OpCompleted(blueprint)
                case Failure(failure) => replyTo ! OpFailed(blueprint, s"ERROR from External Service on $name, ${failure.getClass.getSimpleName}, ${failure.getMessage}, blueprint: ${blueprint.displayId}")
              }
            Behaviors.same
          case adm: AdminOperation => adm match {
            case SetMaxFailures(mf) =>
              active(mf, processingTime, isRandom)
            case SetSpeed(procTime) =>
              active(maxFailures, procTime, isRandom)
            case SetMode(isRand) =>
              active(maxFailures, processingTime, isRand)
          }
        }
      }

      active(MAX_FAILURES, PROCESSING_TIME, IS_RANDOM_MODE)
    }
  }
}