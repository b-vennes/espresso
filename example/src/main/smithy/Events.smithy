$version: "2"

namespace example.events

use example.states#Scores
use example.states#PinballTable

structure Played {
  @required
  score: Long

  @required
  name: String
}

union PinballTableEvent {
  played: Played
}

structure GameStart {
  @required
  time: Timestamp
}

structure TableRemovedFromShop {
  @required
  tableId: Long
}

structure TableAddedToShop {
  @required
  table: PinballTable
}

union ShopUpdateEvent {
  tableRemoved: TableRemovedFromShop
  tableAdded: TableAddedToShop
}

list ShopUpdateEvents {
  member: ShopUpdateEvent
}

structure GameUpdate {
  @required
  time: Timestamp

  @required
  shopEvents: ShopUpdateEvents
}

structure TablePurchased {
  @required
  table: PinballTable
}

structure PurchasedCoffeeBatch {
  @required
  strength: Integer

  @required
  kilos: Integer

  @required
  creditsPerKilo: Integer
}

structure SoldCoffees {
  @required
  batchId: Long

  @required
  scores: Scores
}

union GameEvent {
  start: GameStart
  update: GameUpdate
  tablePurchased: TablePurchased
  purchasedCoffeeBatch: PurchasedCoffeeBatch
}
