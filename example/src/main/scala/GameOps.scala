import cats._
import cats.data._
import cats.effect._
import cats.effect.std._
import cats.mtl._
import cats.syntax.all._
import espresso._
import example.actions._
import example.events._
import example.states._
import monocle.syntax.all._
import smithy4s.Timestamp

import scala.concurrent.duration._

object GameOps {

  given Show[PinballTable] = table => show"""A ${table.name}.
          |Manufactured by ${table.manufacturer}.
          |Looks to be at ${table.currentHealth}%.
          |""".stripMargin

  given [A: Show]: Show[List[A]] = list =>
    list.map(_.show).mkString("", "\n", "")

  given Show[Shop] = shop => show"""Welcome to Odin's Arcade Shop!
          |
          |We have ${shop.available.length} tables available for sale.
          |
          |${shop.available}""".stripMargin

  given Show[Cafe] = cafe => show"""Pincafe!
          |
          |You have ${cafe.tables.length} tables on the floor.
          |
          |${cafe.tables}""".stripMargin

  object GameEvents {
    object Start {
      def unapply(pattern: (Game, GameEvent)): Option[GameStart] =
        pattern match {
          case (_, GameEvent.StartCase(gameStart)) => Some(gameStart)
          case _                                   => None
        }
    }

    object Update {
      def merge(state: Initialized, update: GameUpdate): Initialized =
        update.shopEvents.foldLeft(
          state
            .focus(_.lastUpdated)
            .set(update.time)
            .focus(_.shop.lastUpdated)
            .modify(lastUpdated =>
              if (update.shopEvents.nonEmpty) then update.time
              else lastUpdated
            )
        ) {
          case (
                state,
                ShopUpdateEvent.TableAddedCase(TableAddedToShop(table))
              ) =>
            state.focus(_.shop.available).modify(table :: _)
          case (
                state,
                ShopUpdateEvent.TableRemovedCase(TableRemovedFromShop(id))
              ) =>
            state
              .focus(_.shop.available)
              .modify(_.filter(_.id != id))
        }

      def unapply(
          pattern: (Game, GameEvent)
      ): Option[Initialized] =
        pattern match {
          case (
                Game.InitializedCase(initialized),
                GameEvent.UpdateCase(update)
              ) =>
            Some(merge(initialized, update))
          case _ => None
        }
    }
  }

  def gameEvents[F[_]: Monad] = EventsT[F, GameEvent]
  def gameStateAggregate[F[_]: Monad] = AggregateT(
    gameEvents,
    Game.uninitialized(),
    {
      case GameEvents.Start(GameStart(time)) =>
        Game.initialized(
          Initialized(
            time,
            time,
            1000L,
            Cafe(
              Nil,
              0
            ),
            Shop(time, Nil)
          )
        )
      case GameEvents.Update(game) =>
        Game.initialized(game)
      case (s, _) => s
    }
  )

  def runViewShopInventory[F[_]: Monad](using
      agg: Ask[F, Game],
      console: Console[F]
  ): F[Unit] = for {
    state <- agg.ask
    _ <- state match {
      case Game.InitializedCase(Initialized(_, _, _, _, shop)) =>
        console.println(shop)
      case _ => Monad[F].unit
    }
  } yield ()

  def runViewCafe[F[_]: Monad](using
      agg: Ask[F, Game],
      console: Console[F]
  ): F[Unit] = for {
    state <- agg.ask
    _ <- state match {
      case Game.InitializedCase(Initialized(_, _, _, cafe, _)) =>
        console.println(cafe)
      case _ => Monad[F].unit
    }
  } yield ()

  def runAction[F[_]: Monad](action: GameAction, debugEvents: F[Unit])(using
      events: Tell[F, GameEvent],
      state: Ask[F, Game],
      console: Console[F]
  ): F[Boolean] =
    action match {
      case GameAction.ViewShopInventoryCase =>
        runViewShopInventory.as(true)
      case GameAction.ViewCafeCase =>
        runViewCafe.as(true)
      case GameAction.DebugEventsCase =>
        debugEvents.as(true)
      case GameAction.ExitCase =>
        false.pure[F]
    }

  val updateEvery = 5000.millis

  val updateShopEvery = 30.seconds.toMillis

  def ambientShopUpdate(
      currentTime: Long,
      shop: Shop
  ): Chain[ShopUpdateEvent] = {
    val shopLastUpdated = shop.lastUpdated.epochMilli

    if (currentTime - shopLastUpdated) > updateShopEvery then {
      Chain(
        ShopUpdateEvent.tableAdded(
          TableAddedToShop(
            PinballTable(
              currentTime,
              "Total Nuclear Annihlation",
              "Spooky Pinball",
              95,
              75,
              Nil
            )
          )
        )
      )
    } else {
      Chain.empty
    }
  }

  def ambientUpdate[F[_]: Monad](using
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
                  .pure[F]
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
