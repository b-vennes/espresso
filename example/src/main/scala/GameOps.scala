import cats.*
import cats.effect.*
import cats.effect.std.*
import cats.mtl.*
import cats.syntax.all.*
import espresso.*
import example.actions.*
import example.events.*
import example.states.*
import monocle.macros.*
import monocle.syntax.all.*

object GameOps {

  given Show[PinballTable] = table => show"""A ${table.name}.
          |Manufactured by ${table.manufacturer}.
          |Looks to be at ${table.currentHealth}%.
          |Like it?  It's yours with 'purchase ${table.id}'.
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

  def gameEvents[F[_]: Monad] = EventsT[F, GameEvent]
  def gameStateAggregate[F[_]: Monad](using
    random: Random[F],
    clock: Clock[F]
  ) =
  random.shuffleList(assets.PinballTables.all)
    .map(_.take(3).zipWithIndex)
    .flatMap(_.traverse { case (description, index) =>
      random.betweenInt(25, 100).flatMap(maxHealth =>
        random.betweenInt(1, maxHealth).flatMap(currentHealth =>
          clock.realTime.map(currentTime =>
            PinballTable(
              index + (currentTime.toMillis % 10000),
              description.name,
              description.manufacturer,
              maxHealth,
              currentHealth,
              List.empty
            )
          )
        )
      )
    })
    .map(startingTables =>
      AggregateT(
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
                Shop(time, startingTables)
              )
            )
          case GameEvents.Update(game) =>
            Game.initialized(game)
          case GameEvents.TablePurchased(game) =>
            Game.initialized(game)
          case (s, _) => s
        }
      )
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

  def runPurchaseTable[F[_]: Monad](tableId: Long)(using
      events: Tell[F, GameEvent],
      agg: Ask[F, Game],
      console: Console[F]
  ): F[Unit] = for {
    state <- agg.ask
    maybeGame = GenPrism[Game, Game.InitializedCase]
      .andThen(GenLens[Game.InitializedCase](_.initialized))
      .getOption(state)
    _ <- maybeGame
      .flatMap(game => game.shop.available.find(_.id === tableId))
      .fold(
        console
          .println(s"Sorry pal!  We don't have any tables with ID '$tableId'.")
      )(table =>
        events.tell(
          GameEvent.tablePurchased(
            TablePurchased(table)
          )
        )
      )
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
      case GameAction.PurchaseTableCase(PurchaseTable(tableId)) =>
        runPurchaseTable(tableId).as(true)
      case GameAction.ExitCase =>
        false.pure[F]
    }
}
