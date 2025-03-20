import GameOps.ambientUpdate
import cats._
import cats.data._
import cats.effect._
import cats.effect.std._
import cats.mtl._
import cats.syntax.all._
import espresso._
import espresso.syntax.given
import example.events._
import example.states._
import smithy4s.Timestamp

import scala.concurrent.duration._

object Main extends IOApp.Simple {
  val gameEventsIO = GameOps.gameEvents[IO]
  val gameStateAggregateIO = GameOps.gameStateAggregate[IO]

  type F = StateT[IO, Chain[GameEvent], _]

  override def run: IO[Unit] = for {
    startTime <- IO.realTime
    startEvent = GameEvent.start(
      GameStart(
        Timestamp.fromEpochMilli(startTime.toMillis)
      )
    )
    _ <- Chain(startEvent).tailRecM(events =>
      Input
        .readAction[IO]
        .flatMap { action =>
          given Tell[F, GameEvent] = gameEventsIO.tell
          given Ask[F, Game] = gameStateAggregateIO.ask
          (
            ambientUpdate[F] *>
              GameOps
                .runAction[F](
                  action,
                  StateT.liftF(
                    IO.println(
                      events
                        .map(_.toString)
                        .mkString_("\n")
                    )
                  )
                )
          ).run(events)
            .map {
              case (events, true) => Left(events)
              case (_, right)     => Right(())
            }
        }
    )
  } yield ()
}
