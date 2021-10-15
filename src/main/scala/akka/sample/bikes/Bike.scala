package akka.sample.bikes

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.typed._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, LoggerOps }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityTypeKey }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, Recovery }
import akka.persistence.typed.{ PersistenceId, RecoveryCompleted, SnapshotSelectionCriteria }
import Procurement._
import akka.sample.bikes.tree.GlobalTreeActor
import scala.language.implicitConversions

import scala.concurrent.duration.FiniteDuration

/**
 * Bike FSM.
 */
object Bike {
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Bike")

  sealed trait Event extends CborSerializable
  final case class DownloadEvent(blueprint: Blueprint) extends Event
  final case class DownloadedEvt(blueprint: Blueprint) extends Event
  final case class CreateEvent(blueprint: Blueprint) extends Event
  final case class CreatedEvt(blueprint: Blueprint, location: NiUri) extends Event
  final case class ReserveEvent(blueprint: Blueprint) extends Event
  final case class ReservedEvt(blueprint: Blueprint) extends Event
  final case class YieldEvent(blueprint: Blueprint) extends Event
  final case class YieldedEvt(blueprint: Blueprint) extends Event
  final case class KickEvent(previousState: State) extends Event
  final case class ErrorEvent(errorMessage: String, previousState: State, causeCommand: Command) extends Event

  private case object TimerKey extends CborSerializable

  private implicit def convertState(state: State) = {
    val st = state.getClass.getSimpleName
    if (st.endsWith("$")) st.replace("$", "") else st
  }

  def apply(bikeId: String, bikeTag: String, ops: ActorRef[Operation], globalTreeRef: ActorRef[GlobalTreeActor.TreeCommand],
    shard: ActorRef[ClusterSharding.ShardCommand], numOfShards: Int): Behavior[Command] = {
    implicit val ns = numOfShards
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        context.log.info("STARTING: {}", bikeId)
        val path = fullPath(bikeId, context.system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, InitState)

        val fsmTimeout = context.system.settings.config.getDuration("bikes.fsm-timeout").toMillis
        val timeout = FiniteDuration(fsmTimeout, TimeUnit.MILLISECONDS)
        timers.startSingleTimer(TimerKey, Timeout, timeout)

        val replyToMapper: ActorRef[Reply] = context.messageAdapter(AdaptedReply)

        def active(): Behavior[Command] =
          EventSourcedBehavior[Command, Event, State](
            persistenceId = PersistenceId(typeKey.name, bikeId),
            emptyState = InitState,
            commandHandler(context, ops, replyToMapper, globalTreeRef, shard, bikeId), //commandHandler, given a context, is a function: (State, Operation) => Effect[Event, State],
            eventHandler(context, bikeId))
            .withTagger(a => Set(bikeTag))
            .receiveSignal {
              case (state, RecoveryCompleted) =>
                context.log.info("Bike {} is RECOVERED, entity id {}, state {}", context.self, bikeId, state.getClass.getSimpleName)
                val path = fullPath(bikeId, context.system)
                globalTreeRef ! GlobalTreeActor.AddEntity(path, state)
                Behaviors.same
              case (state, PostStop) =>
                context.log.info("Bike {}\n\t\twith state {}\n\t\twith actor ref {}\n\t\tis now STOPPED", bikeId, state.getClass.getName, context.self)
                val path = fullPath(bikeId, context.system)
                globalTreeRef ! GlobalTreeActor.RemoveEntity(path)
                Behaviors.same
            }.withRecovery(Recovery.withSnapshotSelectionCriteria(SnapshotSelectionCriteria.none))

        // This is a timeout for entities that have been idle for while (no GET requests). It is different from the
        // fsm-timeout, which is used to time out a FSM entity for not reaching its end state in a reasonable time.
        //See https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html?#passivation
        val receiveTimeout = FiniteDuration(
          context.system.settings.config.getDuration("bikes.receive-timeout").toMillis,
          TimeUnit.MILLISECONDS)
        context.setReceiveTimeout(receiveTimeout, Idle)

        active()
      }
    }
  }

  private def commandHandler(context: ActorContext[Command], ops: ActorRef[Operation],
    replyToMapper: ActorRef[Reply], globalTreeRef: ActorRef[GlobalTreeActor.TreeCommand],
    shard: ActorRef[ClusterSharding.ShardCommand], bikeId: String)(implicit numOfShards: Int): (State, Command) => Effect[Event, State] = { (state, command) =>

    def download(cmd: DownloadCmd): Effect[Event, State] = {
      ops ! SomeOperation(cmd.blueprint, replyToMapper, "download()")
      val evt = DownloadEvent(cmd.blueprint)
      Effect.persist(evt).thenRun { newState =>
        val path = fullPath(cmd.blueprint.makeEntityId(), context.system)
        context.log.infoN("Blueprint {} with\n\t\t\tmember {},\n\t\t\tshard {}\n\t\t\tis being downloaded", cmd.blueprint.displayId, path.memberId, path.shardId)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
      }
    }

    def downloaded(reply: Reply): Effect[Event, State] = reply match {
      case OpCompleted(blueprint) =>
        val evt = DownloadedEvt(blueprint)
        Effect.persist(evt).thenRun { newState =>
          context.log.info2("Blueprint {} downloaded, state is now {} ", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          // todo (optional): can possibly use [Replies](https://doc.akka.io/docs/akka/current/typed/persistence.html#replies)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
          context.self ! CreateCmd(blueprint)
        }

      case OpFailed(blueprint, errorMessage) =>
        val evt = ErrorEvent(errorMessage, InitState, DownloadCmd(blueprint))
        Effect.persist(evt).thenRun { newState: State =>
          context.log.info2("ERROR while downloading blueprint {}, state is now {} ", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
        } //.thenStop()
    }

    def create(cmd: CreateCmd): Effect[Event, State] = {
      ops ! SomeOperation(cmd.blueprint, replyToMapper, "create()")
      val evt = CreateEvent(cmd.blueprint)
      Effect.persist(evt).thenRun { newState =>
        context.log.info("Bike {} is being created ", cmd.blueprint.displayId)
        val path = fullPath(bikeId, context.system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
      }
    }

    def created(reply: Reply): Effect[Event, State] = reply match {
      case OpCompleted(blueprint) =>
        //OpCompleted from a real service should contain also location information. Generate randomly here
        val evt = CreatedEvt(blueprint, NiUri(UUID.randomUUID().toString, s"www.bikes.com/locations/${blueprint.makeEntityId()}"))
        Effect.persist(evt).thenRun { newState =>
          context.log.info2("Bike {} created, state is now {} ", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
          context.self ! ReserveCmd
        }

      case OpFailed(blueprint, errorMessage) =>
        val evt = ErrorEvent(errorMessage, DownloadedState(blueprint), CreateCmd(blueprint))
        Effect.persist(evt).thenRun { newState: State =>
          context.log.info2("ERROR while creating bike {}, state is now {} ", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
        }
    }

    def returnState(id: String, state: State, replyTo: ActorRef[BikeRoutesSupport.StatusResponse]): Effect[Event, State] = {
      replyTo ! BikeRoutesSupport.StatusResponse(id, state)
      Effect.none
    }

    def reserve(blueprint: Blueprint, location: NiUri): Effect[Event, State] = {
      ops ! SomeOperation(blueprint, replyToMapper, "reserve()")
      val evt = ReserveEvent(blueprint)
      Effect.persist(evt).thenRun { newState =>
        context.log.info("Bike {} is being reserved ", blueprint.displayId)
        val path = fullPath(bikeId, context.system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
      }
    }

    def reserved(reply: Reply, location: NiUri): Effect[Event, State] = reply match {
      case OpCompleted(blueprint) =>
        val evt = ReservedEvt(blueprint)
        Effect.persist(evt).thenRun { newState =>
          context.log.info2("Bike {} reserved, state is now {}", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
        }

      case OpFailed(blueprint, errorMessage) =>
        val evt = ErrorEvent(errorMessage, CreatedState(blueprint, location), ReserveCmd)
        Effect.persist(evt).thenRun { newState: State =>
          context.log.info2("ERROR while reserving Bike {}, state is now {} ", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
        }
    }

    def `yield`(blueprint: Blueprint): Effect[Event, State] = {
      ops ! SomeOperation(blueprint, replyToMapper, "yield op")
      val evt = YieldEvent(blueprint)
      Effect.persist(evt).thenRun { newState =>
        context.log.info("Bike {} is being yielded ", blueprint.displayId)
        val path = fullPath(bikeId, context.system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
      }
    }

    def yielded(reply: Reply, location: NiUri): Effect[Event, State] = reply match {
      case OpCompleted(blueprint) =>
        val evt = YieldedEvt(blueprint)
        Effect.persist(evt).thenRun { newState =>
          context.log.info2("Bike {} yielded, state is now {}", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
        }

      case OpFailed(blueprint, errorMessage) =>
        val evt = ErrorEvent(errorMessage, ReservedState(blueprint, location), YieldCmd)
        Effect.persist(evt).thenRun { newState: State =>
          context.log.info2("ERROR while reserving bike {}, state is now {} ", blueprint.displayId, newState.getClass.getSimpleName)
          val path = fullPath(blueprint, context.system)
          globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
        }
    }

    def kickIt(commandToReIssue: Command, previousState: State): Effect[Event, State] = {
      val evt = KickEvent(previousState)
      Effect.persist(evt).thenRun { newState =>
        context.log.info2("Blocked bike kicked, state is now {}, commandToReIssue {} ", newState.getClass.getSimpleName, commandToReIssue.getClass.getSimpleName)
        val path = fullPath(bikeId, context.system)
        globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
        context.log.info2("About to reissue command {} from state {} ", commandToReIssue.getClass.getSimpleName, newState.getClass.getSimpleName)
        context.self ! commandToReIssue
      }
    }

    (state, command) match {
      case (_, GetStateCmd(bikeId, replyTo)) =>
        context.log.debug("GET Bike state: {}", state.getClass.getSimpleName)
        returnState(bikeId, state, replyTo)
      case (_, Idle) =>
        context.log.debug("Received IDLE MESSAGE timeout for bike {} ", bikeId)
        shard ! ClusterSharding.Passivate(context.self)
        Effect.none
      case (st, Timeout) =>
        def goBack(stateToGoBackTo: State, commandToReIssue: Command) = {
          context.log.debug("Bike {} has been hanging (not reserved nor yielded) for too long", bikeId)
          val evt = ErrorEvent("Processing took too long: timed out", stateToGoBackTo, commandToReIssue)
          Effect.persist(evt).thenRun { newState: State =>
            context.log.info("Moved bike {} to error state ", bikeId)
            val path = fullPath(bikeId, context.system)
            globalTreeRef ! GlobalTreeActor.AddEntity(path, newState)
          }
        }
        st match {
          case ReservedState(_, _) | YieldedState(_, _) | ErrorState(_, _, _) | InitState => Effect.none
          case DownloadingState(c) => goBack(InitState, DownloadCmd(c))
          case DownloadedState(c) => goBack(DownloadedState(c), CreateCmd(c))
          case CreatingState(c) => goBack(DownloadedState(c), CreateCmd(c))
          case c: CreatedState => goBack(c, ReserveCmd)
          case ReservingState(c, location) => goBack(CreatedState(c, location), ReserveCmd)
          case YieldingState(c, location) => goBack(ReservedState(c, location), YieldCmd)
        }

      case (_, GoodBye) =>
        // the stopMessage, used for rebalance and passivate
        context.log.debug("Received STOP MESSAGE for bike {} ", bikeId)
        val path = fullPath(bikeId, context.system)
        globalTreeRef ! GlobalTreeActor.RemoveEntity(path)
        Effect.stop()

      case (state, command) =>
        state match {
          case InitState =>
            command match {
              case cmd: DownloadCmd => download(cmd)
              case _ => Effect.unhandled
            }

          case _: DownloadingState =>
            command match {
              case AdaptedReply(reply) => downloaded(reply)
              case _ => Effect.unhandled
            }

          case _: DownloadedState =>
            command match {
              case cmd: CreateCmd => create(cmd)
              case _ => Effect.unhandled
            }

          case _: CreatingState =>
            command match {
              case AdaptedReply(reply) => created(reply)
              case _ => Effect.unhandled
            }

          case CreatedState(blueprint, location) =>
            command match {
              case ReserveCmd => reserve(blueprint, location)
              case _ => Effect.unhandled
            }

          case YieldedState(blueprint, location) =>
            command match {
              case ReserveCmd => reserve(blueprint, location)
              case _ => Effect.unhandled
            }

          case ReservingState(_, location) =>
            command match {
              case AdaptedReply(reply) => reserved(reply, location)
              case _ => Effect.unhandled
            }

          case ReservedState(blueprint, _) =>
            command match {
              case YieldCmd => `yield`(blueprint)
              case _ => Effect.unhandled
            }

          case YieldingState(_, location) =>
            command match {
              case AdaptedReply(reply) => yielded(reply, location)
              case _ => Effect.unhandled
            }

          case ErrorState(_, offendingCommand, previousState) =>
            command match {
              case KickCmd => kickIt(offendingCommand, previousState)
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
            case CreateEvent(blueprint) => CreatingState(blueprint)
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case state: CreatingState =>
          event match {
            case CreatedEvt(blueprint, location) => CreatedState(blueprint, location)
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case CreatedState(_, location) =>
          event match {
            case ReserveEvent(blueprint) => ReservingState(blueprint, location)
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case YieldedState(_, location) =>
          event match {
            case ReserveEvent(blueprint) => ReservingState(blueprint, location)
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case ReservingState(_, location) =>
          event match {
            case ReservedEvt(blueprint) => ReservedState(blueprint, location)
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case ReservedState(_, location) =>
          event match {
            case YieldEvent(blueprint) => YieldingState(blueprint, location)
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case YieldingState(_, location) =>
          event match {
            case YieldedEvt(blueprint) => YieldedState(blueprint, location)
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }

        case ErrorState(_, _, _) =>
          event match {
            case KickEvent(previousState) =>
              context.log.debug("Going to state: {}", previousState.getClass.getSimpleName)
              previousState
            case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
          }
      }
    }
  }
}