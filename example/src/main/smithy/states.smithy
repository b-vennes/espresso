$version: "2"

namespace example.states

structure Score {
  @required
  name: String

  @required
  value: Long
}

list Scores {
  member: Score
}

structure PinballTable {
  @required
  id: Long

  @required
  name: String

  @required
  manufacturer: String

  @required
  maxHealth: Integer

  @required
  currentHealth: Integer

  @required
  highScores: Scores
}

list PinballTables {
  member: PinballTable
}

structure Cafe {
  @required
  tables: PinballTables

  @required
  level: Integer
}

structure Shop {
  @required
  lastUpdated: Timestamp

  @required
  available: PinballTables
}

structure Initialized {
  @required
  started: Timestamp

  @required
  lastUpdated: Timestamp

  @required
  credits: Long

  @required
  cafe: Cafe

  @required
  shop: Shop
}

union Game {
  uninitialized: Unit
  initialized: Initialized
}
