import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.std.*
import cats.mtl.*
import cats.syntax.all.*
import espresso.*
import espresso.syntax.given
import example.events.*
import example.states.*
import smithy4s.Timestamp

import scala.concurrent.duration.*

object Main extends IOApp.Simple {
  val gameEventsIO = GameOps.gameEvents[IO]

  type F = StateT[IO, Chain[GameEvent], _]

  override def run: IO[Unit] = for {
    random <- Random.scalaUtilRandom[IO]
    startTime <- IO.realTime
    startEvent = GameEvent.start(
      GameStart(
        Timestamp.fromEpochMilli(startTime.toMillis)
      )
    )
    gameStateAggregateIO <- {
      given Random[IO] = random
      GameOps.gameStateAggregate[IO]
    }
    _ <- Chain(startEvent).tailRecM(events =>
      Input
        .readAction[IO]
        .flatMap { action =>
          given Tell[F, GameEvent] = gameEventsIO.tell
          given Ask[F, Game] = gameStateAggregateIO.ask
          given Random[IO] = random
          (
            Ambient.update[F] *>
              GameOps
                .runAction[F](
                  action,
                  StateT.liftF(
                    IO.println(
                      events
                        .filter {
                          case GameEvent.UpdateCase(update) =>
                            update.shopEvents.nonEmpty
                          case _ => true
                        }
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
