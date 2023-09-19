package espresso

import cats.free.Free
import cats.arrow.FunctionK
import cats.Monad
import cats.syntax.all.*

sealed trait SagaA[A]
case class EmitEvent[S, EV](event: EV, entity: Entity[S, EV]) extends SagaA[Unit]
case class GetEntityState[S, EV](entity: Entity[S, EV]) extends SagaA[S]

type Saga[A] = Free[SagaA, A]

object Saga {
  def emit[S, EV](event: EV, entity: Entity[S, EV]): Saga[Unit] =
    Free.liftF(EmitEvent(event, entity))

  def getState[S, EV](entity: Entity[S, EV]): Saga[S] =
    Free.liftF(GetEntityState(entity))

  def toEventStoreDeltaF[F[_]: Monad]: FunctionK[SagaA, EventStoreDelta.EventStoreDeltaF[F]] =
    new FunctionK[SagaA, EventStoreDelta.EventStoreDeltaF[F]] {
      def apply[A](fa: SagaA[A]): EventStoreDelta.EventStoreDeltaF[F][A] = fa match {
        case EmitEvent(event, entity) => for {
            currentNumber <- EventStoreDelta.getEventNumber[F](entity.streamID)
            _ <- EventStoreDelta.modifyCache[F](
              _.addEvent(entity.streamID, Event(currentNumber + 1, entity.entityType.encode(event)))
                .pure[F]
            )
          } yield ()
        case GetEntityState(entity) => for {
            events <- EventStoreDelta.getEvents[F](entity.streamID)
            state <- EventStoreDelta.pure(EntityType.assemble(entity.entityType, events))
          } yield state
      }
    }

}


extension [A](saga: Saga[A]) {
  def createDelta[F[_]: Monad](eventStore: EventStoreReader[F], cache: EventStoreDelta.EventCache): F[(EventStoreDelta.EventCache, A)] =
    saga.foldMap(Saga.toEventStoreDeltaF[F]).apply(eventStore, cache)
}
