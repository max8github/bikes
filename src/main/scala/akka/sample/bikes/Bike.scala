package akka.sample.bikes

import akka.actor.typed._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, LoggerOps }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityTypeKey }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.{ PersistenceId }
import scala.language.implicitConversions
import scala.language.postfixOps
import akka.actor.typed.ActorRef
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Random, Success }
/**
 * Bike FSM.
 */
object Bike {
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Bike")

  private sealed trait Event extends CborSerializable
  private final case class DownloadEvent(blueprint: String) extends Event
  private final case class DownloadedEvt(blueprint: String) extends Event
  private final case class ErrorEvent(errorMessage: String, previousState: State, causeCommand: Command) extends Event

  private implicit def convertState(state: State) = {
    val st = state.getClass.getSimpleName
    if (st.endsWith("$")) st.replace("$", "") else st
  }

  def apply(bikeId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.info("STARTING: {}", bikeId)

      def active(): Behavior[Command] = EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId(typeKey.name, bikeId),
        emptyState = InitState,
        commandHandler(context, bikeId), //commandHandler, given a context, is a function: (State, Operation) => Effect[Event, State],
        eventHandler(context, bikeId))

      active()
    }
  }

  val random = new Random()
  /** Sets the number of times the external service will fail consecutively before responding. Useful for simulating retries. */
  val MAX_FAILURES = 2
  /** Number of times this actor is willing to try a given operation against an external service. */
  val ATTEMPTS = 3
  /** Processing time of an external operation. Mimics long response times from an external service. */
  val PROCESSING_TIME = 4000L
  val IS_RANDOM_MODE = true
  var currentFailures = 0

  def callExternalService(blueprint: String)(implicit executionContext: ExecutionContext) = {
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

  private def commandHandler(context: ActorContext[Command], bikeId: String): (State, Command) => Effect[Event, State] = { (state, command) =>
    import ExecutionContext.Implicits.global
    def download(cmd: DownloadCmd): Effect[Event, State] = {
      context.pipeToSelf(callExternalService(bikeId)) {
        // map the Future value to a message, handled by this actor
        case Success(_) => OpCompleted(bikeId)
        case Failure(e) => OpFailed(bikeId, "unsure")
      }
      val evt = DownloadEvent(cmd.blueprint)
      Effect.persist(evt)
    }

    def downloaded(reply: Command): Effect[Event, State] = reply match {
      case OpCompleted(blueprint) =>
        val evt = DownloadedEvt(blueprint)
        Effect.persist(evt)

      case OpFailed(blueprint, errorMessage) =>
        val evt = ErrorEvent(errorMessage, InitState, DownloadCmd(blueprint))
        Effect.persist(evt)
    }

    def returnState(id: String, state: State, replyTo: ActorRef[String]): Effect[Event, State] = {
      replyTo ! state.toString
      Effect.none
    }

    (state, command) match {
      case (_, GetStateCmd(bikeId, replyTo)) =>
        context.log.debug("GET Bike state: {}", state.getClass.getSimpleName)
        returnState(bikeId, state, replyTo)

      case (state, command) =>
        state match {
          case InitState =>
            command match {
              case cmd: DownloadCmd => download(cmd)
              case _ => Effect.unhandled
            }

          case _: DownloadingState =>
            command match {
              case c: OpCompleted => downloaded(c)
              case c: OpFailed => downloaded(c)
              case _ => Effect.unhandled
            }

          case _: DownloadedState =>
            command match {
              case _ => Effect.unhandled
            }

          case ErrorState(_, offendingCommand, previousState) =>
            command match {
              case _ => Effect.unhandled
            }
        }
    }
  }

  private def eventHandler(context: ActorContext[Command], bikeId: String): (State, Event) => State = { (state, event) =>
    context.log.debug2("State for {} is now: {}", bikeId, state.getClass.getSimpleName)
    (state, event) match {

      case (_, ErrorEvent(errorMsg, errState, command)) =>
        ErrorState(errorMsg, command, errState)

      case (state, _) => state match {
        case InitState =>
          event match {
            case DownloadEvent(blueprint) => DownloadingState(blueprint);
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case state: DownloadingState =>
          event match {
            case DownloadedEvt(blueprint) =>
              val st = DownloadedState(blueprint)
              context.log.debug("Going to state: {}", st.getClass.getSimpleName)
              st
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case state: DownloadedState =>
          event match {
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case ErrorState(_, _, _) =>
          event match {
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }
      }
    }
  }
}