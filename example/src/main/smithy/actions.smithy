$version: "2"

namespace example.actions

union GameAction {
  viewShopInventory: Unit
  viewCafe: Unit
  exit: Unit
  debugEvents: Unit
}
