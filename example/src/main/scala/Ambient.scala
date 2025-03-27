import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.std.*
import cats.mtl.*
import cats.syntax.all.*
import example.events.*
import example.states.*
import smithy4s.Timestamp

import scala.concurrent.duration.*

object Ambient {

  val updateEvery = 15.seconds

  val updateShopEvery = 45.seconds.toMillis

  def randomTable[F[_]: Monad](
      id: Long
  )(using
      random: Random[F]
  ): F[PinballTable] = for {
    shuffledTables <- random.shuffleList(assets.PinballTables.all)
    tableDescription = shuffledTables.head
    maxHealth <- random.betweenInt(25, 100)
    currentHealth <- random.betweenInt(1, maxHealth)
  } yield PinballTable(
    id,
    tableDescription.name,
    tableDescription.manufacturer,
    maxHealth,
    currentHealth,
    Nil
  )

  def ambientShopUpdate[F[_]: Monad](
      currentTime: Long,
      shop: Shop
  )(using
      random: Random[F]
  ): F[Chain[ShopUpdateEvent]] = {
    val shopLastUpdated = shop.lastUpdated.epochMilli

    if (currentTime - shopLastUpdated) > updateShopEvery then {
      random
        .betweenInt(0, 4)
        .map(_ === 0)
        .ifM(
          randomTable(currentTime % 10000).map(table =>
            Chain(
              ShopUpdateEvent.tableAdded(
                TableAddedToShop(table)
              )
            )
          ),
          Chain.empty.pure[F]
        )
        .flatMap(events =>
          random
            .betweenInt(0, 4)
            .map(_ === 0)
            .ifM(
              random
                .shuffleList(shop.available)
                .map(_.headOption)
                .map {
                  case Some(table) =>
                    Chain(
                      ShopUpdateEvent.tableRemoved(
                        TableRemovedFromShop(table.id)
                      )
                    ) ++ events
                  case None => events
                },
              events.pure[F]
            )
        )
    } else {
      Chain.empty.pure[F]
    }
  }

  def update[F[_]: Monad: Random](using
      events: Tell[F, GameEvent],
      state: Ask[F, Game],
      console: Console[F],
      clock: Clock[F]
  ): F[Unit] = for {
    currentTime <- clock.realTime
    currentTimestamp = currentTime.toMillis
    initialLastUpdated <- state.ask.map {
      case Game.InitializedCase(game) => game.lastUpdated.epochMilli.some
      case _                          => none
    }
    _ <- initialLastUpdated
      .map(
        _.tailRecM(mockTime =>
          if (mockTime > currentTimestamp)
            val mockTimestamp = Timestamp.fromEpochMilli(mockTime)
            ().asRight.pure[F]
          else {
            val mockTimestamp = Timestamp.fromEpochMilli(mockTime)
            state.ask.flatMap {
              case Game.InitializedCase(game) =>
                ambientShopUpdate(mockTime, game.shop)
                  .flatTap(shopEvents =>
                    events.tell(
                      GameEvent.update(
                        GameUpdate(
                          Timestamp.fromEpochMilli(mockTime),
                          shopEvents.toList
                        )
                      )
                    )
                  )
                  .as(Left(mockTime + updateEvery.toMillis))
              case _ => ().asRight.pure[F]
            }
          }
        )
      )
      .getOrElse(().pure[F])
  } yield ()
}
