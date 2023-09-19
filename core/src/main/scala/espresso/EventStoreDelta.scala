package espresso

import cats.syntax.all._
import cats.Monad
import cats.Applicative

trait EventStoreReader[F[_]] {
  def getEvents(stream: String): F[List[Event]]

  def getEventNumber(stream: String): F[Int]
}

case class EventStoreDelta[F[_], A](
  apply: (EventStoreReader[F], EventStoreDelta.EventCache) => F[(EventStoreDelta.EventCache, A)]
)

object EventStoreDelta {

  def useStore[F[_]: Monad, A](f: EventStoreReader[F] => F[A]): EventStoreDelta[F, A] =
    EventStoreDelta[F, A]((es, cache) =>
      f(es).map((cache, _))
    )

  def useCache[F[_]: Monad, A](f: EventCache => F[A]): EventStoreDelta[F, A] =
    EventStoreDelta[F, A]((es, cache) =>
      f(cache).map((cache, _))
    )

  def modifyCache[F[_]: Applicative](f: EventCache => F[EventCache]): EventStoreDelta[F, Unit] =
    EventStoreDelta[F, Unit]((es, cache) =>
      f(cache).map((_, ()))
    )

  def getEventNumber[F[_]: Monad](stream: String): EventStoreDelta[F, Int] =
    for {
      existingNumber <- useStore[F, Int](_.getEventNumber(stream))
      createdNumber <- useCache[F, Int](_.created.get(stream).map(_.length).getOrElse(0).pure[F])
    } yield existingNumber + createdNumber

  def getEvents[F[_]: Monad](stream: String): EventStoreDelta[F, List[Event]] =
    EventStoreDelta((es, cache) =>
      cache.getEvents[F](stream, es)
    )

  def flatMap[F[_], A, B](
    delta: EventStoreDelta[F, A],
    f: A => EventStoreDelta[F, B]
  )(using Monad[F]): EventStoreDelta[F, B] =
    EventStoreDelta[F, B]((es, cache) =>
        delta.apply(es, cache)
          .flatMap {
            case (cache, a) => f(a).apply(es, cache)
          }
    )

  def pure[F[_], A](a: A)(using Applicative[F]): EventStoreDelta[F, A] =
    EventStoreDelta[F, A]((es, cache) => (cache, a).pure[F])

  type EventStoreDeltaF[F[_]] = [X] =>> EventStoreDelta[F, X]

  given [F[_]: Monad]: Monad[EventStoreDeltaF[F]] = new Monad[EventStoreDeltaF[F]] {
    def pure[A](x: A): EventStoreDelta[F, A] =
      EventStoreDelta.pure(x)

    def flatMap[A, B](fa: EventStoreDelta[F, A])(f: A => EventStoreDelta[F, B]): EventStoreDelta[F, B] =
      EventStoreDelta.flatMap(fa, f)

    def tailRecM[A, B](a: A)(f: A => EventStoreDelta[F, Either[A, B]]): EventStoreDelta[F, B] =
      EventStoreDelta[F, B]((es, cache1) =>
          f(a).apply(es, cache1)
            .flatMap {
              case (cache2, result) => 
                result.fold(
                  a => tailRecM(a)(f).apply(es, cache2),
                  b => (cache2, b).pure[F]
                )
            }
      )
  }

  case class EventCache(
    existing: Map[String, List[Event]],
    created: Map[String, List[Event]],
  ) {
    def addEvent(stream: String, event: Event): EventCache =
      EventCache(existing, created.updated(stream, event :: created.getOrElse(stream, Nil)))

    def getEvents[F[_]: Monad](stream: String, store: EventStoreReader[F]): F[(EventCache, List[Event])] =
      for {
        existingEvents <- existing
          .get(stream)
          .fold(store.getEvents(stream).map(_.asLeft))(events => events.asRight.pure[F])
        cache = existingEvents
          .fold(updated => EventCache(existing.updated(stream, updated), created), _ => this)
        events = existingEvents
          .fold(
            events => events,
            existing => existing
          ) ++ created.get(stream).getOrElse(List.empty)
      } yield (cache, events.sortBy(_.number))
  }

  object EventCache {
    def empty: EventCache = EventCache(Map.empty, Map.empty)
  }
}
