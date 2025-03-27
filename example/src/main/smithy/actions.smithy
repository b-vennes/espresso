$version: "2"

namespace example.actions

structure PurchaseTable {
  @required
  tableId: Long
}

union GameAction {
  viewShopInventory: Unit
  viewCafe: Unit
  purchaseTable: PurchaseTable
  exit: Unit
  debugEvents: Unit
}
