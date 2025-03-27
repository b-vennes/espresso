import cats.syntax.all.*
import example.events.*
import example.states.*
import monocle.syntax.all.*

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

  object TablePurchased {
    def merge(game: Initialized, tablePurchased: TablePurchased): Initialized =
      game
        .focus(_.shop.available)
        .modify(_.filterNot(_.id === tablePurchased.table.id))
        .focus(_.cafe.tables)
        .modify(_ :+ tablePurchased.table)

    def unapply(
        pattern: (Game, GameEvent)
    ): Option[Initialized] =
      pattern match {
        case (
              Game.InitializedCase(initialized),
              GameEvent.TablePurchasedCase(tablePurchased)
            ) =>
          Some(merge(initialized, tablePurchased))
        case _ => None
      }
  }
}
